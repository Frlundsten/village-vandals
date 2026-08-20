## ADDED Requirements

### Requirement: A failed fetch is never presented as an empty result
Any view that renders an "empty" state SHALL distinguish "the server said there is nothing" from "the request failed". A failure SHALL render an error, not the empty-state copy.

#### Scenario: Army roster fetch fails
- **WHEN** `GET /unit` rejects
- **THEN** the army view shows an error message
- **AND** it does NOT show "No units yet"

#### Scenario: Buildings list fetch fails
- **WHEN** `GET /building` rejects
- **THEN** the buildings panel shows an error message
- **AND** it does NOT show "No buildings yet"
- **AND** no unhandled promise rejection occurs

#### Scenario: Genuinely empty results still show the empty state
- **WHEN** the server returns an empty roster or an empty building list
- **THEN** the corresponding empty-state message is shown

### Requirement: The army store propagates failures
`useArmyStore.refresh` SHALL reject when the roster fetch fails, so callers can decide how to react. Callers that must not throw SHALL suppress it explicitly at the call site.

#### Scenario: The store surfaces the rejection
- **WHEN** the roster fetch rejects
- **THEN** `refresh` rejects
- **AND** the previously held roster is left unchanged

#### Scenario: The training countdown still degrades quietly
- **WHEN** a roster refresh triggered by a completed training order rejects
- **THEN** the rejection is suppressed at that call site and the countdown continues

### Requirement: The world map handles a failed user fetch
The world map SHALL still render and centre its grid when `GET /user/all` fails, and SHALL show an error message instead of throwing.

#### Scenario: User fetch fails
- **WHEN** `GET /user/all` rejects
- **THEN** the grid is still centred in the viewport
- **AND** an error message is shown
- **AND** no unhandled promise rejection occurs

#### Scenario: Occupied tiles are marked on success
- **WHEN** `GET /user/all` returns villages
- **THEN** the matching tiles are marked as occupied and the grid is centred

### Requirement: Navigation entries never invoke undefined handlers
Every navigation entry SHALL either navigate or be rendered as unavailable. No entry SHALL be wired to a handler that does not exist.

#### Scenario: Unimplemented entries are inert
- **WHEN** the player clicks an unimplemented navigation entry
- **THEN** nothing is thrown
- **AND** the entry is visibly marked as unavailable

### Requirement: The header identifies the player and village
The header SHALL show the authenticated player's username, which `GET /user` returns, and SHALL identify the current village by data that actually exists on the wire.

#### Scenario: Player name is shown after loading
- **WHEN** `GET /user` resolves
- **THEN** the header shows the returned username

#### Scenario: Village is identified without a name field
- **WHEN** the current village is loaded
- **THEN** the header identifies it by its coordinates rather than rendering an empty name

### Requirement: Logout clears all per-account state
Logging out SHALL reset the session, the training queue, the army roster, and the resource totals, so nothing from one account is visible in the next session.

#### Scenario: No carry-over after logout
- **WHEN** the player logs out
- **THEN** the army roster is empty, resource amounts are zero, and the training queue is empty
