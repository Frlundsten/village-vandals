# Task 6: Map bounds clamping

## What
- Extract a `clampMapPosition()` helper that reads `container.getLocalBounds()`, the current scale, and the canvas dimensions, then clamps `container.x` / `container.y` so that at least 20 % of the scaled map width and height remains on-screen.
- Call `clampMapPosition()` at the end of every pan move and every wheel zoom.

## Acceptance Criteria
- AC5: Panning cannot move the map so that < 20 % of its bounding box is on-screen.

## Status
DONE
