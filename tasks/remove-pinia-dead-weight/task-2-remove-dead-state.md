# Task 2 — Remove `user` and `keycloakIdToken` from store state

In `frontend/src/stores/pinia.js`:
- Delete `user: null` from the state factory
- Delete `keycloakIdToken: localStorage.getItem('keycloak_id_token') || null` from the state factory
- Keep `setToken` and `clearSession` localStorage side-effects exactly as-is
