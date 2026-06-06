## ADDED Requirements

### Requirement: Buildings tab navigates to a building overview panel
The home sidebar "Buildings" link SHALL navigate to a `/buildings` route that renders a `BuildingsPanel` component in the main content area, replacing the current no-op click handler.

#### Scenario: Clicking Buildings in sidebar shows the panel
- **WHEN** an authenticated user clicks "Buildings" in the home sidebar
- **THEN** the browser SHALL navigate to `/buildings`
- **THEN** the main content area SHALL render the buildings panel

### Requirement: Buildings panel lists all constructed buildings
The panel SHALL fetch and display all buildings constructed in the player's current village. Each row SHALL show the building icon, building type name, current level, and an upgrade button.

#### Scenario: Panel displays a constructed building row
- **WHEN** the buildings panel mounts and the village has at least one building
- **THEN** each building SHALL be displayed with its icon image, type name, and level number

#### Scenario: Panel shows empty state when no buildings exist
- **WHEN** the buildings panel mounts and the village has no buildings
- **THEN** the panel SHALL display an empty-state message (e.g. "No buildings yet")

### Requirement: Upgrade button reflects affordability
The upgrade button for each building SHALL be enabled when the player's current resource amounts meet or exceed the building's `upgradeCost`, and disabled otherwise. Affordability is checked reactively against the live resource store.

#### Scenario: Upgrade button enabled when player can afford it
- **WHEN** the player has sufficient resources for a building's upgrade cost
- **THEN** the upgrade button for that building SHALL be enabled

#### Scenario: Upgrade button disabled when player cannot afford it
- **WHEN** the player does not have sufficient resources for a building's upgrade cost
- **THEN** the upgrade button for that building SHALL be disabled

### Requirement: Upgrade button triggers upgrade and refreshes state
Clicking an enabled upgrade button SHALL call `POST /building/upgrade`, then refresh both the resource store and the building list to reflect the new state.

#### Scenario: Successful upgrade updates level and resources
- **WHEN** the player clicks an enabled upgrade button
- **THEN** the system SHALL call `POST /building/upgrade` with the correct `villageId` and `constructionSiteId`
- **THEN** the resource store SHALL be refreshed
- **THEN** the building list SHALL be re-fetched and the updated level SHALL be displayed

#### Scenario: Failed upgrade shows error feedback
- **WHEN** the upgrade API call returns an error
- **THEN** an inline error message SHALL be displayed near the affected building row
