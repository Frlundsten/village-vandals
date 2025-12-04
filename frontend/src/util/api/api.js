import { BASE_URL } from '@/util/util.js'
import router from '@/router/index.js'

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('jwt_token')

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  if (token && !path.endsWith('/user/register') && !path.endsWith('/user/auth/generateToken')) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (path.endsWith('/user/register')) {
    await handleNotOkRegister(response)
    return response
  } else {
    await handleNotOkResponse(response)
  }

  return response.json()
}

export function handleNotOkResponse(response) {
  if (!response.ok) {
    if (response.status === 401) {
      router.push('/login')
      return
    }
    throw new Error('Failed to fetch buildings')
  }
}

export async function handleNotOkRegister(response) {
  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || "Registration failed")
  }
}
