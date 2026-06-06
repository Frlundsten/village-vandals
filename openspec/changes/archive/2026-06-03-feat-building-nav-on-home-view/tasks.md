## 1. Tests First (TDD)

- [x] 1.1 Write a failing Vitest component test for `BuildingsPanel.vue`: mounts with a list of buildings and renders one row per building
- [x] 1.2 Write a failing test: upgrade button is disabled when resources are insufficient
- [x] 1.3 Write a failing test: upgrade button is enabled when resources are sufficient

## 2. BuildingsPanel Component

- [x] 2.1 Create `frontend/src/components/BuildingsPanel.vue` with: fetch buildings on mount, show a row per building (icon, name, level, upgrade button), empty-state message when list is empty
- [x] 2.2 Implement affordability computed per building using `useResourceStore`
- [x] 2.3 Implement upgrade handler: call `upgradeBuilding()`, then `resourceStore.refresh()` and re-fetch building list, show inline error on failure

## 3. Router and Sidebar

- [x] 3.1 Add a `/buildings` child route to the home layout in `frontend/src/router/index.js`, pointing to `BuildingsPanel.vue`
- [x] 3.2 Replace the `<a @click="goTo('buildings')">` stub in `Home.vue` sidebar with a `<RouterLink to="/buildings">`

## 4. Verify

- [x] 4.1 Run `npm run test:unit` — all tests green
