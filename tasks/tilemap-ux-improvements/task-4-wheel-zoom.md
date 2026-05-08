# Task 4: Mouse-wheel zoom toward pointer

## What
- Listen to the `wheel` event on `app.stage` (PixiJS v8 FederatedWheelEvent with `deltaY`).
- On each wheel tick: compute a zoom factor (`deltaY < 0` → zoom in, `deltaY > 0` → zoom out).
- Clamp `container.scale.x` (and `.y`) to [0.3, 3].
- Zoom toward the pointer: adjust `container.position` so the world point under the cursor stays fixed after the scale change.
- `app.stage.eventMode = 'static'` is required for the stage to receive wheel events.

## Acceptance Criteria
- AC3: Mouse-wheel zooms the map toward the pointer; scale clamped 0.3–3.

## Status
DONE
