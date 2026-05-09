# Task 1 — Write failing tests for zIndex assignment

Add tests to VillageNew.spec.js asserting:
- `setupSprite` assigns `zIndex = row + col` to a sprite
- `addBuildingSprite` assigns `zIndex = row + col + 0.5` to the building sprite
- The building's zIndex is greater than the base tile's zIndex at the same position

These tests must fail before the implementation changes are made.
