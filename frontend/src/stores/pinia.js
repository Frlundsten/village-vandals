// stores/session.js
import { defineStore } from 'pinia'

export const useSessionStore = defineStore('session', {
    state: () => ({
        user: null,
        isAuthenticated: false,
    }),

    actions: {
        async checkSession() {
            try {
                const response = await fetch('http://localhost:8080/user', {
                    credentials: 'include',
                });

                if (!response.ok) throw new Error('Not authenticated');

                const data = await response.json();
                this.user = data;
                this.isAuthenticated = true;
            } catch (error) {
                this.user = null;
                this.isAuthenticated = false;
            }
        },
    },
});
