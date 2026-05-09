# Task 1 — Write failing tests for the store cleanup

Assert that after the cleanup:
- `useSessionStore` instance does NOT have a `user` property
- `useSessionStore` instance does NOT have a `keycloakIdToken` property
- `setToken` still writes `jwt_token` to localStorage and updates `isAuthenticated`
- `clearSession` still removes `jwt_token`, `keycloak_id_token`, and `villageId` from localStorage

Tests must fail against the current store before any code changes are made.
