## ADDED Requirements

### Requirement: Training order responses carry server time
`TrainingOrderDTO`, as returned by `POST /unit/train` and `GET /unit/training`, SHALL include a `serverTime` field set to the backend's current `Instant` at the moment the response is built.

#### Scenario: Train response includes server time
- **WHEN** `POST /unit/train` succeeds and returns the updated training queue
- **THEN** each `TrainingOrderDTO` in the response includes a `serverTime` field representing the backend's current time

#### Scenario: Training queue fetch includes server time
- **WHEN** `GET /unit/training` returns one or more pending orders
- **THEN** each `TrainingOrderDTO` in the response includes a `serverTime` field representing the backend's current time

### Requirement: Frontend countdown is corrected for client/server clock skew
The training queue countdown in `BuildingUpgradeCard.vue` and the pulsing training indicator driven by `useTrainingQueue.js` SHALL compute remaining time using a clock offset derived from `serverTime`, rather than comparing `finishesAt` directly to the client's uncorrected `Date.now()`.

When a response containing at least one order is received, the consumer SHALL derive `clockOffsetMs = serverTime - Date.now()` (using the first order's `serverTime`) and use `Date.now() + clockOffsetMs` in place of `Date.now()` for all subsequent `remainingMs` calculations against `finishesAt`, until a newer response updates the offset.

#### Scenario: Backend clock lags the client clock
- **GIVEN** the backend clock is 70 seconds behind the client clock
- **AND** a training order is created with `finishesAt` = backend-now + 5 seconds
- **WHEN** the frontend receives the order with its `serverTime`
- **THEN** `remainingMs` is computed as approximately 5000ms (not clamped to 0)
- **AND** the training queue panel remains visible with a live countdown for the full 5 seconds

#### Scenario: Backend clock leads the client clock
- **GIVEN** the backend clock is ahead of the client clock
- **AND** a training order is created with `finishesAt` = backend-now + 5 seconds
- **WHEN** the frontend receives the order with its `serverTime`
- **THEN** `remainingMs` is computed as approximately 5000ms, not inflated by the clock difference

#### Scenario: Countdown completion triggers roster refresh at the correct time
- **GIVEN** the clock-corrected countdown for the active order reaches zero
- **WHEN** the countdown tick detects `remainingMs <= 0`
- **THEN** `armyStore.refresh(villageId)` is called
- **AND** by this point the backend has also completed the order, so the refreshed roster includes the newly trained unit(s)

#### Scenario: Empty training queue does not require server time
- **WHEN** `GET /unit/training` returns an empty list
- **THEN** the frontend does not attempt to derive a clock offset from this response
- **AND** any previously derived offset is left unchanged
