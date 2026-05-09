# Spec: Apply zIndex to building sprites only, not construction site tiles

## Root cause

Setting `sprite.zIndex = row + col` on construction site tiles causes them to depth-sort
ahead of terrain tiles and visually pop out. The tile itself should render in insertion order
just like terrain. Only the building sprite placed on top needs explicit depth sorting.

## Requirements

1. Remove `sprite.zIndex = row + col` from the `isConstructionSiteTile` branch.
2. `addBuildingSprite` keeps `building.zIndex = row + col + 0.5` — unchanged.
3. `container.sortableChildren = true` and `dragLayer.zIndex = -1` — unchanged.

## Acceptance criteria

- No tile sprite (terrain or construction site) has an explicit `zIndex` set.
- Building sprites still have `zIndex = row + col + 0.5`.
- The construction site tile zIndex test is updated to assert `undefined`.
- All tests pass; `npm run build` is clean.

## Architecture impact

- `VillageNew.vue` only — one line removed from the construction-site branch.
- No backend changes, no new files, no schema changes.
