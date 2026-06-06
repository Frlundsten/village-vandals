# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Village Vandals is a full-stack browser strategy game (village builder). Spring Boot 3.5 backend, Vue 3 frontend with PixiJS tile rendering, PostgreSQL database, and Keycloak for authentication. All services run via Docker Compose.

## Commands

### Backend
```bash
mvn clean package          # Build jar
mvn test                   # Run all tests
mvn test -Dtest=ClassName  # Run a single test class
```
Backend runs on port **8081**. Java 21 with `--enable-preview` is required.

### Frontend
```bash
cd frontend
npm install
npm run dev         # Vite dev server → http://localhost:5173
npm run build       # Production build
npm run format      # Prettier formatting
npm run test:unit   # Vitest unit/component tests (run once)
npm run test:unit -- --watch  # Vitest in watch mode (TDD loop)
```

### Full Stack (Docker)
```bash
docker compose up   # Starts PostgreSQL, Spring Boot, Vue/Nginx, Keycloak
```

### Environment Variables
Backend reads from env: `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `SECRET` (JWT secret).
Frontend reads `VITE_API_BASE_URL` from `frontend/.env` (default: `http://localhost:8081`).

## Development Process

### Game Development

When a request involves game development, gameplay mechanics, rendering, animation, scenes, UI/HUD, assets, collisions, input handling, or performance optimization, delegate to the `pixijs-game-dev` agent.

### Required workflow

For EVERY code change:

1. Add or update automated tests that fail before the change.
2. Implement the fix or feature.
3. Run the relevant test suite.
4. Ensure tests pass before completing.

A task is NOT complete unless:
- behavior is covered by tests
- failing reproduction exists for bug fixes
- new functionality has automated test coverage

Never skip tests because a change seems "small".

### Test-Driven Development
Always write tests when implementing or changing functionality. Follow this order:
1. Write a failing test that captures the requirement
2. Implement the minimum code to make it pass
3. Refactor while keeping tests green

Never deliver a feature or bug fix without accompanying tests.

### Mandatory Spec-Driven Development Flow (OpenSpec)

**EVERY feature request and bug fix MUST follow this exact flow. No exceptions.**

#### Step 1 — Propose (STOP and wait for approval)
Run `/opsx:propose <description>` to generate a complete proposal with design, specs, and tasks in one step. Review all artifacts with the user before proceeding.

Do NOT write any code until the user explicitly approves the proposal.

#### Step 2 — Implement with TDD
Run `/opsx:apply` to work through tasks one at a time:
1. Write a failing test that matches an acceptance criterion.
2. Implement the minimum code to pass it.
3. Mark the task complete, then move to the next.

Never begin implementation without an approved proposal.

#### Step 3 — Archive
Run `/opsx:archive` when all tasks are complete. This syncs delta specs to main specs and moves the change to the archive.

Change artifacts live under `openspec/changes/<name>/` while active and `openspec/changes/archive/YYYY-MM-DD-<name>/` when archived.

## Architecture

### Auth Flow

All authentication goes through Keycloak. There is no local password login.

1. Unauthenticated user hits a protected route → router guard redirects to `/login`.
2. `/login` renders `LoginOrRegister.vue` — user clicks Login or Register → redirected to Keycloak.
3. Keycloak redirects back to `/auth?code=...`.
4. `AuthView.vue` POSTs the code to `POST /auth/callback` (public endpoint).
5. Backend exchanges the code with Keycloak, extracts `username` and `email` from the id_token.
6. `UserService.provisionKeycloakUser()` creates a local `User` row on first login; no-ops on subsequent logins.
7. Backend issues an internal short-lived JWT + sets an HTTP-only refresh token cookie.
8. Frontend stores the JWT in `localStorage` (`jwt_token`). `useSessionStore` (Pinia) manages `token` and `user` state.
9. Every API request goes through `apiRequest()` in `frontend/src/util/api/api.js` — injects the JWT as a Bearer token and redirects to `/login` on 401.
10. `JwtAuthFilter` validates the internal JWT on every protected request before reaching controllers.
11. `POST /auth/refresh` (public) — exchanges the HTTP-only refresh cookie for a new JWT and rotates the cookie.
12. `POST /auth/logout` (authenticated) — revokes the refresh token and clears the cookie.

Public endpoints (no JWT required): `POST /auth/callback`, `POST /auth/refresh`.

### Backend Package Structure
Package root: `com.villagevandals.vandals`

| Package | Responsibility |
| --- | --- |
| `web` | Security config, JWT filter/service, auth endpoints, refresh token |
| `user` | User entity (implements `UserDetails`), `/user` endpoints |
| `village` | Village entity, resource production/storage (embedded value objects), village service |
| `building` | Abstract `Building` base + single-table-inheritance subtypes: `Farm`, `LumberMill`, `Forge`, `Brickyard`, `Barrack` |
| `constructionsite` | Tracks buildings currently under construction per village |
| `resource` | Resource types (food/wood/bricks/iron) and production calculations |
| `app` | World map tile entity and coordinate system |
| `unit` | Military/troop system |
| `gameconfig` | Balance constants (costs, production rates, upgrade formulas) |

**Database schema** is managed by Liquibase (YAML changelogs in `src/main/resources/db/changelog/`). Schema evolves via versioned changesets — never modify the database directly.

Buildings use **Hibernate single-table inheritance** (discriminator column `building_type`). The hierarchy is: `Building` (abstract entity) → `AbstractEconomicBuilding implements EconomicProduction` → `Farm`, `LumberMill`, `Forge`, `Brickyard`; and `Building` → `Barrack` (military). The `EconomicProduction` interface (`productionPerHour()`, `producedResource()`) drives how `BuildingService` updates `ResourceProduction` on construct/upgrade — only `instanceof EconomicProduction` buildings change village production rates. Production scales with level: `DEFAULT_ECONOMICAL_PRODUCTION_RATE * level`.

`ConstructionSite` is the indirection layer between a `Village` and its `Building` entities. Each site has a `villageSiteId` (a 1-based sequential number) that matches the order construction-site tiles (GID 59) are encountered during the scan of the Tiled map. The frontend passes this as `constructionSiteId` in API calls; it is set on village initialization, not at build time.

`ResourceStorage` and `ResourceProduction` are **embedded** in `Village` (no separate tables).

### Frontend Structure
- **Router** (`frontend/src/router/index.js`): Route guard checks `useSessionStore().isAuthenticated`; routes with `requiresAuth: true` redirect to `/login` if unauthenticated. Auth callback lands at `/auth`.
- **State**: Two Pinia stores — `useSessionStore` (`stores/pinia.js`) for JWT/auth persisted to localStorage; `useResourceStore` (`stores/resources.js`) for current resource amounts and per-hour rates. Call `resourceStore.refresh(villageId)` to sync both after any game action that changes resources.
- **API layer** (`frontend/src/util/api/`): All backend calls go through `apiRequest()`. Keycloak-specific calls use `keycloak-api.js`.
- **Styling**: TailwindCSS v4 + DaisyUI v5 for utility classes and component theming.
- **Village rendering** (`VillageNew.vue`): PixiJS 8 renders an isometric tile map from `vv.json` (Tiled TMJ format) and `map_tiles.json` (tileset). Construction-site tiles (GID 59, index 58) are identified during scan; `villageSiteId` is the 1-based count of construction-site tiles encountered in scan order across all tile layers. Level badges are HTML `<div>` overlays positioned via PixiJS `getGlobalPosition()`, not PixiJS objects — so they stay at fixed CSS size regardless of map zoom.
- **World map** (`WorldView.vue`): CSS-based isometric grid; tile coordinates stored in `map_tiles` DB table.

### Key Design Decisions
- **Keycloak-only auth**: No local passwords. Keycloak handles identity; the backend issues its own short-lived internal JWT after the OAuth2 callback so the rest of the API is decoupled from Keycloak token format.
- **Stateless JWT + HTTP-only refresh cookie**: JWT for stateless REST; refresh cookie for silent renewal without exposing the long-lived token to JS.
- **Single-table inheritance for buildings**: Simplifies queries but means all building columns live in one table.
- **Async resource production**: Villages track production rates in the DB; resource amounts are calculated from elapsed time (checked on fetch, not via a background job per village). The formula is `stored + rate * (secondsElapsed / 3600.0)` — `DEFAULT_PRODUCTION_PER_HOUR = 3600` is the seconds-per-hour divisor constant, not itself a rate. `DEFAULT_ECONOMICAL_PRODUCTION_RATE = 18000` is the base per-building rate. Before any resource deduction, always call `snapshotCurrentResources()` to commit accumulated resources first; `getCurrentResourceStorage()` computes without writing (display only).
- **CORS**: Allowed origins are `localhost:80` and `localhost:5173` — will need updating for production.

### Code Intelligence

Prefer LSP over Grep/Read for code navigation — it's faster, precise, and avoids reading entire files:
- `workspaceSymbol` to find where something is defined
- `findReferences` to see all usages across the codebase
- `goToDefinition` / `goToImplementation` to jump to source
- `hover` for type info without reading the file

Use Grep only when LSP isn't available or for text/pattern searches (comments, strings, config).

After writing or editing code, check LSP diagnostics and fix errors before proceeding.
