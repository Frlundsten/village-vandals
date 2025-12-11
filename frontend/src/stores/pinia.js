

import { defineStore } from "pinia";

export const useSessionStore = defineStore('session', {
  state: () => ({
    user: null,
    token: localStorage.getItem('jwt_token') || null,
  }),

  getters: {
    isAuthenticated(state) {
      return !!state.token
    }
  },

  actions: {
    setToken(token) {
      this.token = token

      if (token) {
        localStorage.setItem('jwt_token', token)
      } else {
        localStorage.removeItem('jwt_token')
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('jwt_token')
    }
  }
})
