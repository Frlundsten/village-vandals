## ADDED Requirements

### Requirement: Village-scoped endpoints authorize the caller as the village owner
Every endpoint that reads or mutates state belonging to a village SHALL verify that the authenticated principal owns that village before performing any read, mutation, or resource deduction. This applies to `POST /building`, `GET /building`, `GET /resources/refresh`, `POST /unit/train`, `GET /unit/training`, and `GET /unit`. The `villageId` in a request SHALL NOT be trusted on its own.

#### Scenario: Non-owner cannot construct in another player's village
- **GIVEN** village 1 is owned by `alice`
- **WHEN** `bob` sends `POST /building` with `villageId = 1`
- **THEN** the request is rejected with `403`
- **AND** no building is created and no resources are deducted from village 1

#### Scenario: Non-owner cannot train units in another player's village
- **GIVEN** village 1 is owned by `alice` and contains a barrack
- **WHEN** `bob` sends `POST /unit/train` with `villageId = 1`
- **THEN** the request is rejected
- **AND** no training order is created and `alice`'s resources are not deducted

#### Scenario: Non-owner cannot read another player's training queue or roster
- **WHEN** `bob` requests `GET /unit/training` or `GET /unit` for a village owned by `alice`
- **THEN** the request is rejected and no queue or roster data is returned

#### Scenario: Non-owner cannot read or snapshot another player's resources
- **WHEN** `bob` requests `GET /resources/refresh` for a village owned by `alice`
- **THEN** the request is rejected
- **AND** `alice`'s stored resources and `lastUpdate` are left unchanged

#### Scenario: Owner is allowed through
- **WHEN** `alice` calls any village-scoped endpoint for a village she owns
- **THEN** the request proceeds normally

### Requirement: A single ownership check is shared by all village-scoped services
The system SHALL expose one reusable ownership check, `VillageOwnershipService.requireOwner(villageId, username)`, used by every village-scoped service. It SHALL resolve ownership with a single existence projection rather than by loading the `Village` and dereferencing its lazy `owner` association.

#### Scenario: Owner passes the check
- **WHEN** `requireOwner` is called for a village whose owner's username matches
- **THEN** it returns normally

#### Scenario: Non-owner is denied
- **WHEN** `requireOwner` is called for an existing village owned by someone else
- **THEN** it throws `AccessDeniedException`

#### Scenario: Unknown village is a bad request, not a denial
- **WHEN** `requireOwner` is called for a `villageId` that does not exist
- **THEN** it throws `IllegalArgumentException`

### Requirement: Building listing authorizes before deciding what to return
`GET /building` SHALL authorize the caller against the village itself, before and independently of whether that village has any buildings. It SHALL NOT infer ownership from the first construction site it happens to find, and SHALL NOT return an empty result for a village the caller does not own.

#### Scenario: Non-owner listing an empty village is rejected
- **GIVEN** village 1 is owned by `alice` and has no constructed buildings
- **WHEN** `bob` requests `GET /building?villageId=1`
- **THEN** the request is rejected rather than answered with an empty list

#### Scenario: Owner listing an empty village gets an empty list
- **WHEN** `alice` requests `GET /building` for her village that has no constructed buildings
- **THEN** the response is `200 OK` with an empty list

### Requirement: Failures map to accurate HTTP status codes
The system SHALL provide a global exception handler translating domain failures into HTTP status codes: an authorization failure SHALL be `403`, an invalid argument SHALL be `400`, and an invalid state SHALL be `409`. Authorization failures SHALL NOT surface as `500`.

#### Scenario: Authorization failure is 403
- **WHEN** a handler throws `AccessDeniedException`
- **THEN** the response status is `403`

#### Scenario: Invalid argument is 400
- **WHEN** a handler throws `IllegalArgumentException`
- **THEN** the response status is `400`

#### Scenario: Invalid state is 409
- **WHEN** a handler throws `IllegalStateException`
- **THEN** the response status is `409`
