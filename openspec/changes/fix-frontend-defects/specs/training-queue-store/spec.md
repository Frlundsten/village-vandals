## ADDED Requirements

### Requirement: Client batch-size bounds match the server's
The training quantity control SHALL be bounded by the same limits the server enforces: at least 1 and at most `MAX_TRAINING_BATCH_SIZE`. Unit costs and training duration SHALL be declared once, in a shared config module that names the backend file it mirrors, rather than as literals inside the building card.

#### Scenario: Quantity cannot be raised past the server limit
- **WHEN** the player increments the training quantity past `MAX_TRAINING_BATCH_SIZE`
- **THEN** it is clamped to `MAX_TRAINING_BATCH_SIZE`

#### Scenario: Quantity cannot be lowered below one
- **WHEN** the player decrements the training quantity below 1
- **THEN** it is clamped to 1

#### Scenario: Affordability uses the shared cost constants
- **WHEN** batch affordability is evaluated
- **THEN** it uses the shared unit cost constants, not literals redeclared in the component

### Requirement: Stopping the store resets all of its state
`stop()` SHALL clear the countdown, the orders, the cached village id, and the derived clock offset, leaving nothing from the previous session behind.

#### Scenario: Clock offset does not survive a stop
- **GIVEN** a clock offset was derived from a previous session's orders
- **WHEN** `stop()` is called and new orders arrive without a `serverTime`
- **THEN** remaining times are computed against the local clock, not the stale offset
