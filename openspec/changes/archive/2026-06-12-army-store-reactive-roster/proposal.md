## Why

When the barrack training-queue countdown reaches zero, the army tab and the unit-count mini-panel under the Logout button are not updated — both views fetch the roster once on mount and have no mechanism to react to training completion. Introducing a shared Pinia army store (the same pattern as `useResourceStore`) gives a single reactive source of truth that all army-displaying views read from, and lets `useTrainingQueue` trigger a refresh when an order expires.

## What Changes

- Add `frontend/src/stores/army.js` — a Pinia store (`useArmyStore`) with a `roster` ref and a `refresh(villageId)` action that calls `GET /unit`.
- Update `useTrainingQueue.js` — import `useArmyStore`; when `tick()` removes the first expired order, call `armyStore.refresh(villageId)` to trigger lazy promotion and reactive update.
- Update `Home.vue` — replace the local `armyRoster` ref with `useArmyStore`; load the army via `armyStore.refresh(village.id)` inside `loadUserData`.
- Update `ArmyView.vue` — replace the local `roster` ref with `useArmyStore`; call `armyStore.refresh(villageId)` on mount instead of managing local state.
- Add store unit tests for `useArmyStore`.
- Update `useTrainingQueue.spec.js` to assert `armyStore.refresh` is called when an order expires.

## Capabilities

### New Capabilities

- `army-roster-store`: A reactive Pinia store that holds the village unit roster and can be refreshed from any component or composable. Replaces the local per-component roster refs in Home.vue and ArmyView.vue.

### Modified Capabilities

*(none — training-queue-display spec is unchanged; the store is an implementation detail that satisfies the existing spec requirement)*

## Impact

- **New file**: `frontend/src/stores/army.js`
- **Modified files**: `frontend/src/composables/useTrainingQueue.js`, `frontend/src/views/Home.vue`, `frontend/src/views/ArmyView.vue`
- **New test file**: `frontend/src/stores/__tests__/army.spec.js`
- **Updated test file**: `frontend/src/composables/__tests__/useTrainingQueue.spec.js`
- **Backend**: No changes — `GET /unit` already triggers lazy promotion via `resolveCompletedOrders`.
- **No new npm dependencies**.
