# Spec: Remove dead weight from `useSessionStore`

## Requirements

1. Remove the `user` field from the store — it is declared in state, assigned `null`, and never written to or read from anywhere in the codebase.
2. Remove `keycloakIdToken` from the store's reactive state — it is only read in one place (`Home.vue` logout handler) and can be sourced directly from `localStorage.getItem('keycloak_id_token')` there instead.
3. `setToken` must continue to write `keycloakIdToken` to localStorage (the Keycloak logout path still depends on that key existing in localStorage).
4. `clearSession` must continue to remove `keycloak_id_token` from localStorage.
5. All existing behaviour — login, logout, Keycloak SSO flow, route guard — must remain identical.

## Acceptance criteria

- `useSessionStore` state no longer contains `user` or `keycloakIdToken` fields.
- `Home.vue` reads `keycloakIdToken` directly from `localStorage` at logout time instead of from the store.
- All Vitest tests for the store pass; a new test asserts `user` and `keycloakIdToken` are not present on the store instance.
- `npm run build` produces no type/lint errors.

## Architecture impact

- `frontend/src/stores/pinia.js` — remove `user` and `keycloakIdToken` from state; `setToken` and `clearSession` keep their localStorage side-effects unchanged.
- `frontend/src/views/Home.vue` — replace `session.keycloakIdToken` with `localStorage.getItem('keycloak_id_token')`.
- No backend changes, no schema changes, no new endpoints.

## Out of scope

- Removing the Keycloak logout flow itself.
- Refactoring `setToken` or `clearSession` signatures.
- Moving token storage away from localStorage.
- Any changes to `apiRequest`, the router guard, `LoginView`, or `AuthView`.
