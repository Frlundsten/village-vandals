## ADDED Requirements

### Requirement: Demolish a building from a construction site

The system SHALL provide a `DELETE /building` endpoint that removes the building occupying a construction site, identified by `villageId` and `constructionSiteId`, and frees the site for future construction.

#### Scenario: Owner demolishes a building successfully

- **WHEN** the authenticated owner of the village requests `DELETE /building` with a valid `villageId` and a `constructionSiteId` that has a building
- **THEN** the building is removed, the construction site is left unoccupied, and the endpoint responds `200 OK` with a success message

#### Scenario: Demolishing frees the site for reuse

- **WHEN** a building is demolished from a construction site
- **THEN** that site's building reference is cleared so a new building can later be constructed on the same site

### Requirement: Only the village owner may demolish a building

The system SHALL reject demolition requests from any user who is not the owner of the village containing the construction site.

#### Scenario: Non-owner is rejected

- **WHEN** a user who does not own the village requests demolition of a building in that village
- **THEN** the system rejects the request and the building is not removed

### Requirement: Demolition requires an existing building

The system SHALL reject a demolition request for a construction site that has no building.

#### Scenario: Empty site cannot be demolished

- **WHEN** the owner requests demolition of a construction site that has no building
- **THEN** the system rejects the request and no production or persistence change occurs

#### Scenario: Unknown site is rejected

- **WHEN** the owner requests demolition with a `constructionSiteId`/`villageId` pair that does not resolve to a construction site
- **THEN** the system rejects the request

### Requirement: Demolishing an economic building reverses its production

The system SHALL subtract the demolished building's current production-per-hour from the village's production rate for the resource it produced, and SHALL commit accumulated resources before changing the rate.

#### Scenario: Economic building production is reversed

- **WHEN** the owner demolishes an economic building producing R units per hour of a resource
- **THEN** the village's per-hour production rate for that resource is reduced by R

#### Scenario: Resources are snapshotted before the rate changes

- **WHEN** an economic building is demolished
- **THEN** the village's accumulated resources are snapshotted (committed) before the production rate is reduced

#### Scenario: Non-economic building demolition leaves production unchanged

- **WHEN** the owner demolishes a non-economic building (e.g. a Barrack)
- **THEN** the village's production rates are unchanged
