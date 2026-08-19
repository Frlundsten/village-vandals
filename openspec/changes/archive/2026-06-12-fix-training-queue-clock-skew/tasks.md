## 1. Backend: add `serverTime` to TrainingOrderDTO

- [x] 1.1 Update `UnitControllerTest` to assert `$[0].serverTime` is present in the JSON response for `getTrainingQueue_returnsOrdersSortedByFinishesAt`, `trainUnit_validRequest_returnsQueueWithNewOrder`, and `trainUnit_batchRequest_passesQuantityThroughAndReturnsBatchOrder`
- [x] 1.2 Add `serverTime` (`Instant`) field to `TrainingOrderDTO`
- [x] 1.3 Update `UnitService.toOrderDTOs()` to populate `serverTime` with `Instant.now()` for every order in the response
- [x] 1.4 Update any other `TrainingOrderDTO` construction sites (e.g. `UnitServiceTest` fixtures) to match the new record signature
- [x] 1.5 Run `mvn test -Dtest=UnitControllerTest,UnitServiceTest` and confirm green

## 2. Frontend: clock-offset-corrected countdown in BuildingUpgradeCard

- [x] 2.1 Add a failing test in `BuildingUpgradeCard.spec.js`: given `trainUnit` resolves with an order whose `finishesAt` is in the past relative to `Date.now()` but `serverTime` makes it ~5s in the future, the training queue panel SHALL remain visible with a countdown near 5.0s (not disappear within one tick)
- [x] 2.2 Add a failing test for `fetchTrainingQueue` (mount-time GET) with the same skewed-clock setup, asserting the queue panel renders and stays visible
- [x] 2.3 Update `enrichOrder()` in `BuildingUpgradeCard.vue` to derive/apply a `clockOffsetMs` from `order.serverTime` and use `Date.now() + clockOffsetMs` for `remainingMs`
- [x] 2.4 Update `startCountdown()`'s tick to use the corrected clock for recomputing `remainingMs` each interval
- [x] 2.5 Ensure the offset is recomputed whenever a non-empty order list is received (mount fetch and POST /train response), and left unchanged for empty responses
- [x] 2.6 Run `npm run test:unit` for `BuildingUpgradeCard.spec.js` and confirm green, including pre-existing tests

## 3. Frontend: clock-offset-corrected countdown in useTrainingQueue

- [x] 3.1 Add a failing test in `useTrainingQueue.spec.js`: with a skewed `serverTime`/`finishesAt` pair (finishesAt appears past per raw `Date.now()` but ~5s future per `serverTime`), `hasActiveFor(buildingId)` SHALL remain `true` until the corrected countdown actually elapses
- [x] 3.2 Update `enrichOrder()` and `tick()` in `useTrainingQueue.js` to derive/apply the same `clockOffsetMs` pattern as `BuildingUpgradeCard.vue`
- [x] 3.3 Run `npm run test:unit` for `useTrainingQueue.spec.js` and confirm green

## 4. Verification

- [x] 4.1 Run the full backend suite (`mvn test`) and full frontend suite (`npm run test:unit`) to confirm no regressions
- [x] 4.2 Manually verify in the running app: train a Vandal, confirm the queue panel + countdown + progress bar stay visible for the full duration, the map's pulsing indicator shows, and the army mini-panel updates when the countdown completes without switching tabs
