## Why

Players can construct and upgrade buildings, but there is no way to remove one once placed. A construction site stays permanently occupied, so a misplaced or no-longer-wanted building blocks that slot forever. The backend needs the ability to demolish a building and free its construction site.

## What Changes

- Add a `DELETE /building` endpoint to `BuildingController` that accepts `villageId` and `constructionSiteId` and is authenticated to the village owner via `Principal`.
- Add a `deleteBuilding` method to `BuildingService` that:
  - Validates the village and construction site exist and that the authenticated user owns the village (reuse `validateOwner`).
  - Ensures a building actually exists at the site (otherwise rejects the request).
  - Snapshots current resources before any production-rate change so accumulated resources are committed.
  - If the building is an `EconomicProduction`, reverses its production contribution by subtracting its current `productionPerHour()` from the village's `ResourceProduction`.
  - Clears the building from the `ConstructionSite`, freeing the slot, and persists the change.
- Backend only — no frontend, no refund of construction cost (out of scope).

## Capabilities

### New Capabilities
- `building-demolition`: Demolishing a building from a construction site, with owner authorization, production-rate reversal for economic buildings, and freeing the site for reuse.

### Modified Capabilities
<!-- None — no existing spec's requirements change. -->

## Impact

- **Code**: `BuildingController` (new DELETE mapping), `BuildingService` (new `deleteBuilding` method), `ConstructionSite` (cleared building reference), reuse of `ResourcesService.snapshotCurrentResources` and a production-reversal path.
- **API**: New `DELETE /building` endpoint (protected, owner-only).
- **Persistence**: `ConstructionSite` row updated to null out its building; the `Building` row is removed via the cleared association.
- **No schema migration** required (uses existing tables/associations).
- **Tests**: New JUnit/Mockito coverage for the service method and controller endpoint.
