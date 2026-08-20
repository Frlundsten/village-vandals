## ADDED Requirements

### Requirement: Resource amounts advance in real time between syncs
The resource store SHALL accrue production locally from elapsed wall-clock time, using the same rule as the server: credit whole units and carry the fractional remainder to the next tick. The displayed amounts SHALL move without a page action, and SHALL stay correct when the browser throttles timers in a background tab.

#### Scenario: Amounts increase while the player watches
- **GIVEN** the store has been synced with a per-hour rate above zero
- **WHEN** time passes
- **THEN** the corresponding amount increases according to that rate

#### Scenario: The fractional remainder is not lost
- **GIVEN** a rate that produces less than one unit per tick
- **WHEN** enough ticks pass to accumulate a whole unit
- **THEN** the amount increases by that unit

#### Scenario: A throttled tab does not lose production
- **GIVEN** ticks are delayed far beyond their nominal interval
- **WHEN** the next tick runs
- **THEN** production is credited for the real elapsed time, not for one nominal interval

#### Scenario: A zero rate never changes the amount
- **GIVEN** a per-hour rate of zero
- **WHEN** time passes
- **THEN** the amount is unchanged

### Requirement: A server sync is authoritative
`refresh(villageId)` SHALL replace amounts and rates with the server's values and reset the local accrual, so the local projection can never drift permanently.

#### Scenario: Refresh overwrites a local projection
- **GIVEN** the local projection has advanced past the last synced amount
- **WHEN** `refresh` returns different amounts from the server
- **THEN** the store holds the server's amounts, not the projected ones

#### Scenario: Accrual restarts from the sync point
- **WHEN** `refresh` completes
- **THEN** subsequent accrual is measured from that moment, not from the previous sync

### Requirement: The store can be reset
The store SHALL expose a reset that stops accrual and returns all amounts and rates to zero, so one account's totals never survive into the next session.

#### Scenario: Logout clears resource totals
- **WHEN** the player logs out
- **THEN** the resource amounts and rates are zero and no accrual timer is left running

### Requirement: The current village id falls back correctly
Resolving the current village id SHALL treat `0` as "not yet known" and fall back to the stored id. The player SHALL NOT be able to navigate to a village route with id `0`, and a resource refresh SHALL NOT be issued for it.

#### Scenario: Falling back before the user request resolves
- **GIVEN** the current village is still at its initial id of `0`
- **AND** a village id is present in local storage
- **WHEN** the current village id is resolved
- **THEN** it is the stored id, not `0`

#### Scenario: No refresh for an unknown village
- **GIVEN** neither a loaded village nor a stored id is available
- **WHEN** the resource UI update is triggered
- **THEN** no request is made and no unhandled rejection occurs
