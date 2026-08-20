## ADDED Requirements

### Requirement: Snapshotting resources credits and consumes exactly the same elapsed time
When resources are snapshotted, the system SHALL credit production for the whole seconds elapsed since `lastUpdate` and SHALL advance `lastUpdate` by exactly those seconds — not to the current instant. The sub-second remainder SHALL carry forward to the next snapshot, so repeated snapshots never discard production time.

#### Scenario: Two snapshots inside the same second still credit the full elapsed time
- **GIVEN** a village whose `lastUpdate` was 1.5 seconds ago
- **WHEN** resources are snapshotted twice in quick succession, the second snapshot 0.2 s after the first
- **THEN** the total credited production equals one whole second at the first snapshot
- **AND** the remaining 0.7 s is still pending, so a later snapshot credits it rather than losing it

#### Scenario: A sub-second snapshot leaves the clock untouched
- **GIVEN** a village whose `lastUpdate` was 0.4 seconds ago
- **WHEN** resources are snapshotted
- **THEN** no production is credited
- **AND** `lastUpdate` is unchanged, so the 0.4 s is not lost

#### Scenario: A whole-second snapshot advances the clock by that many seconds
- **GIVEN** a village whose `lastUpdate` was exactly 10 seconds ago
- **WHEN** resources are snapshotted
- **THEN** production for 10 seconds is credited
- **AND** `lastUpdate` has advanced by 10 seconds

### Requirement: Elapsed time is never negative
If `lastUpdate` is in the future relative to the current instant (clock adjustment, clock skew, or a bad write), the system SHALL treat the elapsed time as zero. Production SHALL never be subtracted from a village's stored resources.

#### Scenario: A future lastUpdate does not remove resources
- **GIVEN** a village whose `lastUpdate` is one hour in the future
- **WHEN** the current resource amounts are calculated
- **THEN** the amounts equal the stored amounts, unchanged
- **AND** no negative production is applied

### Requirement: Production accumulation cannot overflow into a negative balance
Accumulated production SHALL be computed in 64-bit arithmetic and the resulting stored amount SHALL saturate at `Integer.MAX_VALUE` rather than wrapping. A village left idle for a long time at a high production rate SHALL NOT end up with a negative resource balance.

#### Scenario: A very long idle period saturates instead of wrapping
- **GIVEN** a village with a high production rate whose `lastUpdate` is many years in the past
- **WHEN** the current resource amounts are calculated
- **THEN** each amount is `Integer.MAX_VALUE` at most
- **AND** no amount is negative
