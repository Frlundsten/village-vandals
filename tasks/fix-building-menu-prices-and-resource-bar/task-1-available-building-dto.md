# Task 1 — Add AvailableBuildingDTO and update available buildings endpoint

## Goal
Create `AvailableBuildingDTO(String type, Map<String, Integer> constructionCost)` record.
Update `BuildingController.getAvailableBuildings` to return `List<AvailableBuildingDTO>`,
mapping each building's `getConstructionCost()` with lowercase string keys.

## Files changed
- `src/main/java/com/villagevandals/vandals/building/dto/AvailableBuildingDTO.java` — new
- `src/main/java/com/villagevandals/vandals/building/BuildingController.java`

## Status: TODO
