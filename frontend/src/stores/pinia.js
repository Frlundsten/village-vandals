import {defineStore} from "pinia";

export const useSessionStore = defineStore('session', {
    state: () => ({
        user: null,
        token: localStorage.getItem('jwt_token'),
        isAuthenticated: !!localStorage.getItem('jwt_token'),
    }),

    actions: {
        setToken(token) {
            this.token = token;
            this.isAuthenticated = !!token;
            if (token) {
                localStorage.setItem('jwt_token', token);
            } else {
                localStorage.removeItem('jwt_token');
            }
        },

        async logout() {
            this.user = null;
            this.token = null;
            this.isAuthenticated = false;
            localStorage.removeItem('jwt_token');
        },
    },
})
