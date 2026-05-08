# Task 2: Update handleBuildingSelection in VillageNew

## What
- Import `constructBuilding` in `VillageNew.vue`.
- Make `handleBuildingSelection` async.
- Await `constructBuilding`, then `fetchBuildings` to update `buildingsBySiteId`.
- Only call `addBuildingSprite` after both succeed.
- On error: log and return without adding the sprite.

## Acceptance Criteria
- AC1: Clicking a newly built building sprite opens the upgrade card immediately.
- AC2: `buildingsBySiteId` is populated before the sprite is rendered.
- AC3: API failure prevents sprite from being added.
- AC4: Pre-existing buildings still work.

## Status
DONE — implemented before spec was written.
