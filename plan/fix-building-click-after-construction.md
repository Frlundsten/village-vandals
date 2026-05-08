# Spec: Fix Building Click After Construction

## Requirements
- After a player selects a construction site and builds a building, clicking the newly placed building sprite must immediately open the upgrade card — without requiring a browser refresh.
- The construction API call must complete and the building must be persisted in the backend before the sprite is added to the canvas.
- If the construction API call fails, no building sprite should be added.

## Acceptance Criteria
- AC1: Clicking a newly built building sprite immediately opens `BuildingUpgradeCard` with the correct building data.
- AC2: `buildingsBySiteId` is updated with the new building before the sprite's `pointerup` handler can fire.
- AC3: If `constructBuilding` throws, no sprite is added and an error is logged.
- AC4: The upgrade flow continues to work for buildings that existed before page load.

## Architecture Impact
- **`BuildingMenu.vue`**: `sendInfo` no longer calls `constructBuilding`. It only emits `buildingType` and `closeMenu`. The `constructBuilding` import is removed.
- **`VillageNew.vue`**: `handleBuildingSelection` becomes `async`. It calls `constructBuilding`, then `fetchBuildings` to refresh `buildingsBySiteId`, then calls `addBuildingSprite`. Imports `constructBuilding`.
- No backend changes. No schema changes. No new endpoints.

## Out of Scope
- Error UI feedback to the user when construction fails (currently only `console.error`).
- Optimistic UI (sprite shown before API confirms).
- Any changes to the upgrade flow itself.
