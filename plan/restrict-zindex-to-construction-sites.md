# Spec: Restrict isometric zIndex to construction sites and building sprites only

## Root cause

`setupSprite` sets `sprite.zIndex = row + col` on every tile sprite including terrain. For
terrain tiles, insertion order (row-major) was already correct isometric depth ordering —
adding `zIndex` is unnecessary and, for multi-layer maps, causes tiles from different layers
that share the same `row + col` value to interleave in unexpected ways.

The `zIndex` fix was only needed to sort building sprites correctly relative to each other.
Construction site tiles are the anchor points for buildings, so they also need `zIndex` so a
construction site and its building always sort together against neighbouring sites.

## Requirements

1. Remove `sprite.zIndex = row + col` from `setupSprite`.
2. Inside the `if (isConstructionSiteTile(gid))` branch, set `sprite.zIndex = row + col` on
   that specific tile sprite only.
3. `addBuildingSprite` keeps `building.zIndex = row + col + 0.5` — unchanged.
4. `container.sortableChildren = true` and `dragLayer.zIndex = -1` — unchanged.

## Acceptance criteria

- Terrain tile sprites have no explicit `zIndex` set (remain at default 0).
- Construction site tile sprites have `zIndex = row + col`.
- Building sprites have `zIndex = row + col + 0.5`.
- The setupSprite zIndex test is updated: a plain tile has `zIndex` of `0` (not explicitly set).
- A new test asserts a construction site tile sprite gets `zIndex = row + col`.
- All existing tests pass; `npm run build` is clean.

## Architecture impact

- `VillageNew.vue` only — one line removed from `setupSprite`, one line added inside the
  construction-site branch.
- No backend changes, no new files, no schema changes.

## Out of scope

- Terrain elevation or multi-layer depth sorting beyond insertion order.
