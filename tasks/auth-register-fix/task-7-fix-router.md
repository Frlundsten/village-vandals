# Task 7 — Fix router (add /register route)

## Problem
`RegisterView.vue` is imported in `router/index.js` but no route points to it. `LoginView.vue` links to `/register` which 404s.

## Fix
Add a `/register` route rendering `RegisterView`. Keep `/login` pointing to `LoginOrRegister.vue` (Keycloak primary flow).

## Files changed
- `frontend/src/router/index.js`

## Status: DONE
