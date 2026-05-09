# Spec: Wire FORGE and BRICKYARD tile sprites to their building types

## Findings

- The code in `VillageNew.vue:182` and `VillageNew.vue:293` loads tile sprites as `/assets/Tiles/${building.type}.png`.
- **FORGE**: `/assets/Tiles/FORGE.png` exists. Backend fully supports FORGE. Works end-to-end with no changes needed.
- **BRICKYARD**: `/assets/Tiles/BRICKYARD.png` does **not** exist. The file that was placed is `Tiles/MASONRY.png` (typo in filename). The backend fully supports BRICKYARD already.
- **MASONRY** is not a real building type and requires no backend work.

## Requirements

1. Rename `public/assets/Tiles/MASONRY.png` → `public/assets/Tiles/BRICKYARD.png` so the existing tile-sprite loading logic resolves correctly for placed BRICKYARD buildings.
2. No code changes needed in `VillageNew.vue`, `BuildingPresentationCard.vue`, or any backend file.

## Acceptance criteria

- `/assets/Tiles/BRICKYARD.png` is served by the dev server and production build.
- `/assets/Tiles/MASONRY.png` no longer exists (avoids confusion with a non-existent building type).
- A new Vitest test asserts that `handleBuildingSelection('BRICKYARD')` triggers `Assets.load` with `/assets/Tiles/BRICKYARD.png`.
- All existing tests continue to pass.

## Architecture impact

- One file renamed in `frontend/public/assets/Tiles/`.
- One new test case added to `VillageNew.spec.js`.
- No backend changes, no schema changes, no new endpoints.

## Out of scope

- FORGE (already works).
- Any new building types.
- The building menu icon for BRICKYARD (`/assets/BRICKYARD.png` already exists and is correct).
