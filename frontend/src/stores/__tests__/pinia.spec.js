import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSessionStore } from '@/stores/pinia.js'

describe('useSessionStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('does not expose a user field', () => {
    const session = useSessionStore()
    expect('user' in session).toBe(false)
  })

  it('does not expose a keycloakIdToken field', () => {
    const session = useSessionStore()
    expect('keycloakIdToken' in session).toBe(false)
  })

  it('isAuthenticated is false when no token in localStorage', () => {
    const session = useSessionStore()
    expect(session.isAuthenticated).toBe(false)
  })

  it('setToken writes jwt_token to localStorage and sets isAuthenticated true', () => {
    const session = useSessionStore()
    session.setToken('my-jwt')
    expect(localStorage.getItem('jwt_token')).toBe('my-jwt')
    expect(session.isAuthenticated).toBe(true)
  })

  it('setToken writes keycloak_id_token to localStorage when provided', () => {
    const session = useSessionStore()
    session.setToken('my-jwt', 'my-keycloak-id-token')
    expect(localStorage.getItem('keycloak_id_token')).toBe('my-keycloak-id-token')
  })

  it('setToken removes keycloak_id_token from localStorage when not provided', () => {
    localStorage.setItem('keycloak_id_token', 'old-token')
    const session = useSessionStore()
    session.setToken('my-jwt')
    expect(localStorage.getItem('keycloak_id_token')).toBeNull()
  })

  it('clearSession removes jwt_token, keycloak_id_token and villageId from localStorage', () => {
    localStorage.setItem('jwt_token', 'tok')
    localStorage.setItem('keycloak_id_token', 'kc-tok')
    localStorage.setItem('villageId', '42')
    const session = useSessionStore()
    session.clearSession()
    expect(localStorage.getItem('jwt_token')).toBeNull()
    expect(localStorage.getItem('keycloak_id_token')).toBeNull()
    expect(localStorage.getItem('villageId')).toBeNull()
    expect(session.isAuthenticated).toBe(false)
  })
})
