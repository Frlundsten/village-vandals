# Task 2 — Fix Keycloak redirect_uri in LoginOrRegister.vue

## Problem
`REDIRECT_URI` is `http://localhost:8081/auth/callback` — Keycloak sends the auth code to the backend directly, returning raw JSON to the browser. `AuthView.vue` (at `/auth`) never receives the code.

## Fix
Change `REDIRECT_URI` to `http://localhost:5173/auth` so Keycloak redirects to the frontend.

## Files changed
- `frontend/src/views/LoginOrRegister.vue`

## Status: DONE
