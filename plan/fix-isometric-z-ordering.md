# Spec: Fix isometric z-ordering for building sprites

## Root cause

In isometric rendering, sprites with a higher `row + col` sum are visually closer to the viewer and must be drawn last (on top). Currently:

- Base tiles are added in row-major order, which produces correct depth by accident.
- Building sprites are added with `container.addChild(building)` (`VillageNew.vue:315`), which always appends to the top of the display list — so the most recently placed building always covers everything regardless of position.

## Requirements

1. Enable `container.sortableChildren = true` so PixiJS sorts all children by `zIndex` before each render.
2. Assign `zIndex = row + col` to every base tile sprite in `setupSprite`.
3. Assign `zIndex = row + col + 0.5` to every building sprite in `addBuildingSprite` so a building always floats above its own base tile but sorts correctly relative to other tiles and buildings.
4. Assign `zIndex = -1` to the `dragLayer` so it stays permanently beneath everything.

## Acceptance criteria

- A building placed at a lower `row + col` position is visually behind a building at a higher `row + col` position, regardless of construction order.
- The drag layer continues to capture pan events without interfering with tile or building clicks.
- Existing tests pass; a new test asserts that `addBuildingSprite` assigns a `zIndex` greater than that of its base tile at the same position.

## Architecture impact

- `frontend/src/components/VillageNew.vue` only — four targeted changes: `container`, `setupSprite`, `addBuildingSprite`, and `dragLayer`.
- No backend changes, no new files, no schema changes.

## Out of scope

- Sorting within the same `row + col` diagonal.
- Fixing z-ordering for the drag layer event propagation beyond keeping it at `zIndex = -1`.
