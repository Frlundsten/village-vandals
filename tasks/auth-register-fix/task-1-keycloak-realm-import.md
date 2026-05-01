# Task 1 — Keycloak realm auto-import

## Goal
Make Keycloak auto-configure the `villagevandals` realm and `backend-service` client on every fresh start, so the correct redirect URI (`http://localhost:5173/auth`) is always registered without manual admin steps.

## Files changed
- `keycloak/villagevandals-realm.json` — new realm definition
- `Dockerfile.keycloak` — copy realm file into image, pass `--import-realm` to start-dev
- `compose.yaml` — add `KEYCLOAK_BASE_URL` env var to backend service

## Status: DONE
