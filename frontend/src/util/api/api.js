import { BASE_URL } from '@/util/util.js'
import router from '@/router/index.js'
import { useSessionStore } from '@/stores/pinia.js'
import { ApiError } from '@/util/api/apiError.js'

/**
 * In-flight refresh, shared by every caller that hits a 401 at the same time.
 *
 * The backend rotates the refresh cookie on each use, so a burst of concurrent refreshes would
 * see all but the first present an already-revoked token. Home's mount alone fires four requests
 * in a row, so this is the common case, not an edge case.
 */
let refreshPromise = null

const STATUS_FALLBACKS = {
  400: 'The server rejected that request',
  401: 'Your session has expired',
  403: 'You do not have access to that',
  404: 'Not found',
  409: 'That is not possible right now',
}

function isAuthPath(path) {
  return path.startsWith('/auth/')
}

function send(path, options) {
  const token = localStorage.getItem('jwt_token')

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  return fetch(`${BASE_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    // The refresh token lives in an HTTP-only cookie scoped to /auth/refresh.
    credentials: 'include',
    body: options.body ? JSON.stringify(options.body) : undefined,
  })
}

/** Resolves to a new access token, or null if the session cannot be renewed. */
function attemptRefresh() {
  if (!refreshPromise) {
    refreshPromise = fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    })
      .then(async (response) => {
        if (!response.ok) return null
        const body = await readJson(response)
        return body?.accessToken ?? null
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function endSession() {
  useSessionStore().clearSession()
  router.push('/login')
}

/**
 * Sends a request with the stored JWT attached.
 *
 * On a 401 it tries once to renew the access token through `/auth/refresh` and replays the
 * request; only if that fails does the session end. Any non-2xx response rejects with an
 * {@link ApiError} carrying the status and the reason the server sent.
 */
export async function apiRequest(path, options = {}) {
  let response = await send(path, options)

  if (response.status === 401 && !isAuthPath(path)) {
    const freshToken = await attemptRefresh()
    if (freshToken) {
      useSessionStore().setToken(freshToken, localStorage.getItem('keycloak_id_token'))
      response = await send(path, options)
    }
  }

  if (!response.ok) {
    if (response.status === 401) {
      endSession()
    }
    throw new ApiError(await readErrorMessage(response), response.status)
  }

  return readJson(response)
}

async function readText(response) {
  try {
    return await response.text()
  } catch {
    return ''
  }
}

/** Parses a success body, tolerating an empty one (204-style responses). */
async function readJson(response) {
  const text = await readText(response)
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

/**
 * Pulls the reason out of an error body.
 *
 * The backend does not speak a single shape: the global exception handler returns
 * `{"message":{"message":"..."}}`, some endpoints return a bare string, and Spring's own error
 * page returns `{"timestamp":...,"error":"..."}`. Anything unrecognised falls back to the raw
 * text, then to a status-specific default.
 */
async function readErrorMessage(response) {
  const fallback = STATUS_FALLBACKS[response.status] ?? `Request failed (${response.status})`
  const text = await readText(response)
  if (!text) return fallback

  let body
  try {
    body = JSON.parse(text)
  } catch {
    return text
  }

  return firstString(body?.message?.message, body?.message, body?.error, body) ?? fallback
}

function firstString(...candidates) {
  return candidates.find((candidate) => typeof candidate === 'string' && candidate.length > 0)
}
