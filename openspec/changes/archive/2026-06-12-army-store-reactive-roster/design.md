## Context

`Home.vue` holds `armyRoster` as a plain `ref([])` fetched once inside `loadUserData()`. `ArmyView.vue` holds its own `roster` ref fetched once in `onMounted`. These are entirely independent — neither knows about the other, and neither is connected to the training queue. The `useTrainingQueue` composable (after the simplify-training-queue change) removes expired orders from the client-side queue but makes no network call and has no way to notify views that new units are available.

The project already has a precedent for this pattern: `useResourceStore` (Pinia) holds resource amounts and rates; any component can call `resourceStore.refresh(villageId)` and every view that reads from the store updates reactively. The army roster needs the same treatment.

## Goals / Non-Goals

**Goals:**
- Army tab and the Home.vue mini-panel update reactively when training completes, without a page refresh or manual navigation.
- A single `useArmyStore.refresh(villageId)` call from `useTrainingQueue.tick()` is sufficient to update all army-displaying views.
- The store is the sole source of truth for the roster — no component manages its own local roster ref.

**Non-Goals:**
- Real-time push / WebSocket roster updates.
- Per-unit-type reactive counts (the store exposes the full roster array, just as `GET /unit` returns it).
- Changes to backend endpoints or the lazy-promotion mechanism.
- `BuildingUpgradeCard` displaying the updated roster (it is not an army-displaying view).

## Decisions

### 1. Pinia store mirroring useResourceStore

**Decision**: `useArmyStore` is a Pinia composition-style store (`defineStore('army', () => { … })`) with a `roster` ref and a `refresh(villageId)` action that calls `fetchRoster(villageId)`.

**Why**: Mirrors the established `useResourceStore` pattern. Pinia stores are module-level singletons — all components reading `armyStore.roster` get the same reactive ref, so a single `refresh` call updates every subscribing view simultaneously. No new state-management infrastructure is needed.

**Alternative considered**: Emitting a custom event from `useTrainingQueue` that `Home.vue` and `ArmyView.vue` listen to. Rejected — Vue's event system is component-scoped; wiring cross-component events from a composable requires either a shared event bus or `provide/inject`, both of which add complexity for no benefit over a Pinia store.

### 2. useTrainingQueue calls armyStore.refresh on order expiry

**Decision**: Inside `tick()`, after filtering expired orders from the queue, call `armyStore.refresh(villageId)` as a fire-and-forget call (errors caught and discarded).

**Why**: The composable already knows the `villageId` and is the only place in the frontend that detects order expiry. Calling the store refresh here is a one-line addition. The `GET /unit` call it triggers runs `resolveCompletedOrders` on the backend, promoting the completed `TrainingOrder` to real `VillageUnit` rows.

**Alternative considered**: A watcher in `Home.vue` on `useTrainingQueue().queue` length to detect when it decreases. Rejected — `Home.vue` does not currently use `useTrainingQueue`, adding it just for a watcher is heavier than the store approach, and `ArmyView.vue` would still need its own solution.

### 3. ArmyView reads from store but also triggers refresh on mount

**Decision**: `ArmyView.vue` calls `armyStore.refresh(villageId)` on mount and reads `armyStore.roster` for display. If the store already has data from a prior refresh (e.g., triggered by `tick()`), the mount refresh still runs to ensure accuracy.

**Why**: The user may navigate to the army view before any training completes, or at any time after a page load where the store is empty. The mount refresh guarantees the view is current regardless of when it was last refreshed by the composable. Calling refresh on mount is idempotent — it just overwrites the roster with fresh data.

## Risks / Trade-offs

- **Double refresh**: If a training order expires while `ArmyView.vue` is mounted, both `tick()` and a potential navigation/scroll into view could trigger `refresh()` near-simultaneously. Result is two identical GET calls; the second overwrites with the same data. → Acceptable; no state corruption.
- **Empty store on first visit**: If the user opens the army view before `Home.vue` has loaded (e.g., deep link), `armyStore.roster` starts empty and the mount-time refresh fills it. → Handled by the mount refresh in `ArmyView.vue`.
- **armyStore.refresh errors**: Network failures are caught and discarded; `roster` retains its last valid value. → Same degradation behaviour as the current per-component approach.

## Migration Plan

1. Create `army.js` store and its tests — no other files change yet.
2. Update `useTrainingQueue.js` and its tests — composable now calls the store.
3. Update `Home.vue` to read from the store — remove local `armyRoster` ref.
4. Update `ArmyView.vue` to read from the store — remove local `roster` ref.
5. No backend changes; no database migration; no deployment coordination.
6. Rollback: revert the four frontend files; no data is affected.
