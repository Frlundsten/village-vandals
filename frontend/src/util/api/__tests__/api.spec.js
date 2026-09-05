import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/router/index.js', () => ({
  default: { push: vi.fn() },
}))

import router from '@/router/index.js'
import { apiRequest } from '@/util/api/api.js'
import { ApiError } from '@/util/api/apiError.js'
import { useSessionStore } from '@/stores/pinia.js'

/** Minimal stand-in for the parts of `Response` that apiRequest touches. */
function response(status, body = '') {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  }
}

describe('apiRequest', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    localStorage.setItem('jwt_token', 'old-token')
    globalThis.fetch = vi.fn()
  })

  it('returns parsed JSON and sends the bearer token', async () => {
    fetch.mockResolvedValueOnce(response(200, { villages: [{ id: 7 }] }))

    const data = await apiRequest('/user')

    expect(data).toEqual({ villages: [{ id: 7 }] })
    const [, init] = fetch.mock.calls[0]
    expect(init.headers.Authorization).toBe('Bearer old-token')
  })

  it('resolves rather than rejecting when a 200 has an empty body', async () => {
    fetch.mockResolvedValueOnce(response(200, ''))

    await expect(apiRequest('/auth/logout', { method: 'POST' })).resolves.not.toThrow()
  })

  // --- refresh on 401 ---

  it('refreshes the token on a 401 and replays the original request once', async () => {
    fetch
      .mockResolvedValueOnce(response(401, ''))
      .mockResolvedValueOnce(response(200, { accessToken: 'fresh-token' }))
      .mockResolvedValueOnce(response(200, { villages: [] }))

    const data = await apiRequest('/user')

    expect(data).toEqual({ villages: [] })
    expect(fetch.mock.calls[1][0]).toContain('/auth/refresh')
    expect(localStorage.getItem('jwt_token')).toBe('fresh-token')
    expect(fetch.mock.calls[2][1].headers.Authorization).toBe('Bearer fresh-token')
    expect(router.push).not.toHaveBeenCalled()
  })

  it('does not refresh or replay a second time when the replay also 401s', async () => {
    fetch
      .mockResolvedValueOnce(response(401, ''))
      .mockResolvedValueOnce(response(200, { accessToken: 'fresh-token' }))
      .mockResolvedValueOnce(response(401, ''))

    await expect(apiRequest('/user')).rejects.toMatchObject({ status: 401 })

    expect(fetch).toHaveBeenCalledTimes(3)
  })

  it('clears the session and routes to /login when the refresh fails', async () => {
    fetch.mockResolvedValueOnce(response(401, '')).mockResolvedValueOnce(response(401, ''))

    const session = useSessionStore()

    await expect(apiRequest('/user')).rejects.toBeInstanceOf(ApiError)

    expect(localStorage.getItem('jwt_token')).toBeNull()
    expect(session.isAuthenticated).toBe(false)
    expect(router.push).toHaveBeenCalledWith('/login')
  })

  it('rejects with an auth error, not a JSON parse error, on an unrecoverable 401', async () => {
    fetch.mockResolvedValueOnce(response(401, '')).mockResolvedValueOnce(response(401, ''))

    const error = await apiRequest('/user').catch((e) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(401)
    expect(error.message).not.toMatch(/JSON/i)
  })

  it('issues exactly one refresh for concurrent 401s', async () => {
    fetch.mockImplementation((url) => {
      if (String(url).includes('/auth/refresh')) {
        return Promise.resolve(response(200, { accessToken: 'fresh-token' }))
      }
      if (localStorage.getItem('jwt_token') === 'fresh-token') {
        return Promise.resolve(response(200, { ok: true }))
      }
      return Promise.resolve(response(401, ''))
    })

    await Promise.all([apiRequest('/user'), apiRequest('/unit'), apiRequest('/building')])

    const refreshCalls = fetch.mock.calls.filter(([url]) => String(url).includes('/auth/refresh'))
    expect(refreshCalls).toHaveLength(1)
  })

  it('does not attempt a refresh for an auth path', async () => {
    fetch.mockResolvedValueOnce(response(401, ''))

    await expect(apiRequest('/auth/logout', { method: 'POST' })).rejects.toMatchObject({
      status: 401,
    })

    expect(fetch).toHaveBeenCalledTimes(1)
  })

  // --- error messages ---

  it('extracts the reason from the global handler body shape', async () => {
    fetch.mockResolvedValueOnce(
      response(400, { message: { message: 'Insufficient wood: need 60, have 10' } }),
    )

    await expect(apiRequest('/building', { method: 'POST' })).rejects.toMatchObject({
      status: 400,
      message: 'Insufficient wood: need 60, have 10',
    })
  })

  it('extracts the reason from a bare JSON string body', async () => {
    fetch.mockResolvedValueOnce(response(400, '"Quantity must be between 1 and 50, was 999"'))

    await expect(apiRequest('/unit/train', { method: 'POST' })).rejects.toMatchObject({
      message: 'Quantity must be between 1 and 50, was 999',
    })
  })

  it('uses a plain-text body as the reason', async () => {
    fetch.mockResolvedValueOnce(response(403, 'Not the owner of village 1'))

    await expect(apiRequest('/building')).rejects.toMatchObject({
      status: 403,
      message: 'Not the owner of village 1',
    })
  })

  it('still produces a usable error when the body is empty', async () => {
    fetch.mockResolvedValueOnce(response(500, ''))

    const error = await apiRequest('/building').catch((e) => e)

    expect(error.status).toBe(500)
    expect(error.message).toBeTruthy()
  })

  it('surfaces a network failure as an error', async () => {
    fetch.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    await expect(apiRequest('/user')).rejects.toThrow()
  })
})
