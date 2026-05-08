# Task 3: Write tests

## What
Write unit/component tests covering the acceptance criteria.

### Tests to write
1. `handleBuildingSelection` — after successful construction, `buildingsBySiteId` contains the new building (AC2).
2. `handleBuildingSelection` — after successful construction, `addBuildingSprite` is called (AC1 setup).
3. `handleBuildingSelection` — if `constructBuilding` throws, `addBuildingSprite` is NOT called (AC3).
4. `BuildingMenu.sendInfo` — emits `buildingType` and `closeMenu` without calling the buildings API (AC3 / task 1).

## Status
DONE
