# Spec: Prevent map drag when press originates outside the canvas

## Root cause

`pointerDownFn` is a native `pointerdown` listener on `app.canvas`. When the user clicks on
an HTML overlay (like the BuildingMenu card), that event is consumed by the HTML element and
does **not** reach the canvas — so `dragStart`/`containerStart` are not updated and `dragging`
is not reset.

The `globalpointermove` handler on the PixiJS drag layer fires for all pointer movement over
the canvas and uses `event.buttons !== 0` as its only guard. If the user holds a button on the
menu card and then moves the mouse, `event.buttons` is non-zero and the handler tries to drag
using a stale `dragStart`.

## Requirements

1. Introduce a `dragInitiatedOnCanvas` boolean (initially `false`).
2. Set it `true` in `pointerDownFn` (only fires for genuine canvas presses).
3. Set it `false` in `pointerUpFn`.
4. In the `globalpointermove` handler, bail out immediately if `dragInitiatedOnCanvas` is
   `false` — before checking `event.buttons` or computing deltas.

## Acceptance criteria

- Holding the mouse button on the BuildingMenu and moving the mouse does not pan the map.
- Holding the mouse button on the canvas and moving does still pan the map correctly.
- The drag-threshold test (< 5 px movement does not activate dragging) continues to pass.
- A new test asserts that `globalpointermove` does not pan the map when `dragInitiatedOnCanvas`
  is false.
- All existing tests pass.

## Architecture impact

- `frontend/src/components/VillageNew.vue` only — one new local variable, three one-line edits.
- No backend changes, no new files, no schema changes.

## Out of scope

- Blocking scroll/zoom on the canvas while the menu is open.
- Closing the menu on canvas click.
