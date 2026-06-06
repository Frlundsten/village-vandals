## Why

The "Buildings" tab in the home sidebar currently does nothing (`goTo('buildings')` logs to console). Players have no way to see their constructed buildings at a glance or trigger upgrades from a central panel. This feature wires up the Buildings tab to a dedicated panel showing all buildings in the current village with their level, icon, and an upgrade button that reflects current affordability.

## What Changes

- Add a new `BuildingsPanel.vue` component that lists all constructed buildings in the village.
- Each row shows: building icon (`/assets/<TYPE>.png`), building type name, current level, and an Upgrade button.
- The Upgrade button is enabled only when the player can afford the next level (checked against live resource store amounts); disabled otherwise.
- Clicking Upgrade calls `POST /building/upgrade`, refreshes resources and building list, shows inline success/error feedback.
- Add a `/buildings` child route under the home layout and wire the sidebar "Buildings" `<a>` to a `<RouterLink>` pointing to it.
- No backend changes required — `GET /building?villageId` and `POST /building/upgrade` already exist and return all needed data.

## Capabilities

### New Capabilities
- `building-overview-panel`: A buildings panel accessible from the home sidebar, showing all village buildings with upgrade affordability and one-click upgrade.

### Modified Capabilities

## Impact

- **Frontend only**: new `BuildingsPanel.vue` component, router update, `Home.vue` sidebar link change.
- `GET /building?villageId` is already authenticated and returns `BuildingDTO` (type, level, upgradeCost, constructionSiteId, productionPerHour).
- `useResourceStore` provides live resource amounts for affordability checks.
- Building images already exist at `public/assets/<TYPE>.png` (LUMBERMILL.png, FARM.png, FORGE.png, BRICKYARD.png, BARRACK.png).
