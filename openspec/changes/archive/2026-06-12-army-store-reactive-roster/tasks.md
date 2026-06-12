## 1. useArmyStore — create and test

- [x] 1.1 Write failing test: `refresh_populatesRoster_fromFetchRoster` — calling `armyStore.refresh(villageId)` fetches the unit list and sets `roster`
- [x] 1.2 Create `frontend/src/stores/army.js` — Pinia composition store with `roster` ref and `refresh(villageId)` action calling `fetchRoster`
- [x] 1.3 Write failing test: `refresh_setsEmptyArray_whenNoUnits` — `refresh` with an empty response sets `roster` to `[]`
- [x] 1.4 Run `npm run test:unit` — army store tests pass

## 2. useTrainingQueue — call armyStore.refresh on order expiry

- [x] 2.1 Write failing test: `tick_callsArmyStoreRefresh_whenFirstOrderExpires` — after advancing timers past `finishesAt`, `armyStore.refresh` has been called once with the correct villageId
- [x] 2.2 Update `useTrainingQueue.js` — import and use `useArmyStore`; call `armyStore.refresh(villageId)` fire-and-forget when the first expired order is removed in `tick()`
- [x] 2.3 Run `npm run test:unit` — all composable tests pass

## 3. Home.vue — read roster from store

- [x] 3.1 In `Home.vue`, replace the local `armyRoster` ref with `useArmyStore`; replace the `fetchRoster` call in `loadUserData` with `armyStore.refresh(village.id)`; read `armyStore.roster` in the template
- [x] 3.2 Remove the `fetchRoster` import from `Home.vue` (no longer needed directly)
- [x] 3.3 Run `npm run test:unit` — no regressions

## 4. ArmyView.vue — read roster from store

- [x] 4.1 In `ArmyView.vue`, replace the local `roster` ref with `useArmyStore`; replace `fetchRoster` in `onMounted` with `armyStore.refresh(villageId)`
- [x] 4.2 Remove the `fetchRoster` import from `ArmyView.vue`
- [x] 4.3 Run `npm run test:unit` — all tests pass (51 or more)

## 5. BuildingUpgradeCard — local queue state (race condition fix)

- [x] 5.1 Replace `useTrainingQueue` in `BuildingUpgradeCard.vue` with local `trainingOrders` ref + `mountFetchCancelled` flag; manage countdown interval directly; call `armyStore.refresh` on expiry
- [x] 5.2 All 3 new failing tests pass: queue visible after Train, queue survives stale GET, fetchRoster called on expiry
