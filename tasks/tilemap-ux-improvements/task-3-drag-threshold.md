# Task 3: Replace timer-based drag with movement-threshold drag

## What
- Remove `dragTimer`, `DRAG_DELAY`, and the `setTimeout` / `clearTimeout` logic.
- On `pointerdown`: record `dragStart` and `containerStart`; set `dragging = false`.
- On `globalpointermove` (PixiJS v8 — replaces `pointermove` on the drag layer): if the pointer has moved > 5 px from `dragStart`, set `dragging = true` and pan the container.
- On `pointerup` / `pointerupoutside`: reset `dragging = false`. A click is only registered if `dragging` was never set to true.
- The tile `pointerup` event on construction-site sprites must also guard against drag: only open the menu if `dragging === false`.
- Use `eventMode = 'static'` on the drag layer (PixiJS v8 replaces `interactive = true`).

## Acceptance Criteria
- AC2: Clicking a construction-site tile opens BuildingMenu; dragging does not trigger it.

## Status
DONE
