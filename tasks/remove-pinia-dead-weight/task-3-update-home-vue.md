# Task 3 — Update Home.vue to read keycloakIdToken from localStorage

In `frontend/src/views/Home.vue`:
- In `handleLogout`, replace `const keycloakIdToken = session.keycloakIdToken` with
  `const keycloakIdToken = localStorage.getItem('keycloak_id_token')`
- Remove any `storeToRefs` or direct access to `session.keycloakIdToken`
