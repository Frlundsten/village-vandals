# Task 7: Tests

## What
Unit tests covering the pure/logic pieces of the new behaviour.

### Tests to write
1. `clampMapPosition` — position too far left → clamped so 20% is visible.
2. `clampMapPosition` — position too far right → clamped so 20% is visible.
3. `clampMapPosition` — position in valid range → unchanged.
4. `handleBuildingSelection` (existing test suite) — still passes after refactor.
5. `VillageNew` — `loading` ref starts `true` and is `false` after `flushPromises()`.

## Files
- `frontend/src/components/__tests__/VillageNew.spec.js`

## Status
DONE
