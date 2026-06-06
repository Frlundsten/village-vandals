import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('jwt_token') || null,
  }),

  getters: {
    /** True when a JWT is present in state. Does not verify expiry — the backend enforces that. */
    isAuthenticated(state) {
      return !!state.token
    },
  },

  actions: {
    /**
     * Stores the JWT and optional Keycloak id_token in state and localStorage.
     * Passing null for a value removes it from localStorage.
     * @param {string|null} token - Internal JWT issued by the backend
     * @param {string|null} keycloakIdToken - Keycloak id_token, present only after SSO login
     */
    setToken(token, keycloakIdToken = null) {
      this.token = token

      if (token) {
        localStorage.setItem('jwt_token', token)
      } else {
        localStorage.removeItem('jwt_token')
      }

      if (keycloakIdToken) {
        localStorage.setItem('keycloak_id_token', keycloakIdToken)
      } else {
        localStorage.removeItem('keycloak_id_token')
      }
    },

    /**
     * Wipes all auth state from memory and localStorage, including the stored villageId.
     * Call on logout or when the backend returns a 401.
     */
    clearSession() {
      this.token = null
      localStorage.removeItem('jwt_token')
      localStorage.removeItem('keycloak_id_token')
      localStorage.removeItem('villageId')
    },
  },
})
