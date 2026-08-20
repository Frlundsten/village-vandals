## MODIFIED Requirements

### Requirement: Frontend countdown is corrected for client/server clock skew
The training queue countdown and the pulsing training indicator on the village map SHALL compute remaining time using a clock offset derived from `serverTime`, rather than comparing `finishesAt` directly to the client's uncorrected `Date.now()`.

The clock offset SHALL be owned solely by `useTrainingStore`. When a response containing at least one order is received — from `GET /unit/training` during hydration or from `POST /unit/train` — the store SHALL derive `clockOffsetMs = serverTime - Date.now()` (using the first order's `serverTime`) and use `Date.now() + clockOffsetMs` in place of `Date.now()` for all subsequent `remainingMs` calculations against `finishesAt`, until a newer response updates the offset. Components SHALL read the corrected `remainingMs` from the store and SHALL NOT derive or hold their own offset.

#### Scenario: Backend clock lags the client clock
- **GIVEN** the backend clock is 70 seconds behind the client clock
- **AND** a training order is created with `finishesAt` = backend-now + 5 seconds
- **WHEN** the store receives the order with its `serverTime`
- **THEN** `remainingMs` is computed as approximately 5000ms (not clamped to 0)
- **AND** the training queue panel remains visible with a live countdown for the full 5 seconds

#### Scenario: Backend clock leads the client clock
- **GIVEN** the backend clock is ahead of the client clock
- **AND** a training order is created with `finishesAt` = backend-now + 5 seconds
- **WHEN** the store receives the order with its `serverTime`
- **THEN** `remainingMs` is computed as approximately 5000ms, not inflated by the clock difference

#### Scenario: Countdown completion triggers roster refresh at the correct time
- **GIVEN** the clock-corrected countdown for the active order reaches zero
- **WHEN** the store's countdown tick detects `remainingMs <= 0`
- **THEN** `armyStore.refresh(villageId)` is called
- **AND** by this point the backend has also completed the order, so the refreshed roster includes the newly trained unit(s)

#### Scenario: Empty training queue does not require server time
- **WHEN** `GET /unit/training` returns an empty list
- **THEN** the store does not attempt to derive a clock offset from this response
- **AND** any previously derived offset is left unchanged

#### Scenario: Offset survives closing the building card
- **GIVEN** a clock offset has been derived and an order is counting down
- **WHEN** the player closes the building card
- **THEN** the offset is retained by the store
- **AND** subsequent ticks continue to use it
