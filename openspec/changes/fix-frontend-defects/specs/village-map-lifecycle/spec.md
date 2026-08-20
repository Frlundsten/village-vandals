## ADDED Requirements

### Requirement: The map renders even when the buildings fetch fails
Rendering the village map SHALL NOT be gated on `GET /building`. If that call fails, the map SHALL still render and the view SHALL show an error message. The player SHALL never be left with a blank screen and no explanation.

#### Scenario: Buildings fetch fails
- **WHEN** `GET /building` rejects during mount
- **THEN** the tile map is still rendered
- **AND** an error message is shown
- **AND** the loading overlay is dismissed

#### Scenario: Map rendering itself fails
- **WHEN** the map or tileset assets fail to load
- **THEN** the loading overlay is dismissed and an error message is shown rather than an empty container

### Requirement: An aborted mount leaks nothing
The asynchronous mount SHALL check for unmount after each await and SHALL tear down anything it created. Teardown SHALL be idempotent and SHALL release the PixiJS application, the canvas listeners, and the window resize listener exactly once, whichever of mount and unmount finishes last.

#### Scenario: Navigating away mid-mount
- **GIVEN** the player leaves the village view before the mount chain completes
- **WHEN** the suspended mount resumes
- **THEN** the PixiJS application it created is destroyed
- **AND** no `resize` listener remains registered on the window

#### Scenario: The resize listener is never registered after removal
- **WHEN** the component unmounts before the mount finishes
- **THEN** the mount does not go on to add a window `resize` listener

#### Scenario: Normal unmount tears everything down
- **WHEN** a fully mounted village view unmounts
- **THEN** the PixiJS application is destroyed, canvas listeners are removed, and the window `resize` listener is removed

### Requirement: Resizing fits the map to the new canvas size
On window resize the map SHALL be scaled and centred using the dimensions the canvas is being resized *to*, not the dimensions it had before the event. PixiJS defers its own renderer resize to the next animation frame, so the renderer's reported size is stale during the event.

#### Scenario: Shrinking the window
- **WHEN** the window is resized smaller
- **THEN** the map is rescaled and re-centred to the new viewport, not the previous one

#### Scenario: Repeated resizes stay correct
- **WHEN** the window is resized several times in a row
- **THEN** each fit reflects the size after that resize, never lagging one step behind

### Requirement: A failed upgrade is reported and keeps the card open
When an upgrade request fails, the building card SHALL stay open and SHALL display the reason. It SHALL NOT close as though the upgrade succeeded.

#### Scenario: Upgrade rejected by the server
- **WHEN** the upgrade request rejects
- **THEN** the card remains open showing the reason from the server
- **AND** the building's level badge is unchanged

#### Scenario: Successful upgrade still closes the card
- **WHEN** the upgrade succeeds
- **THEN** the card closes and the level badge reflects the new level

### Requirement: A failed construction is reported
When constructing a building fails, the view SHALL surface the reason rather than only logging it.

#### Scenario: Construction rejected
- **WHEN** the construction request rejects
- **THEN** an error message is shown and no building sprite is added
