# Task 1 — Write failing test for BRICKYARD tile sprite path

Add a Vitest test to `VillageNew.spec.js` asserting that `handleBuildingSelection('BRICKYARD')`
triggers `Assets.load` with `/assets/Tiles/BRICKYARD.png`.

This test must fail before the rename is done (because the file doesn't exist yet, and
there is no existing test covering BRICKYARD specifically).
