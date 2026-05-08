# Task 1: Fix canvas container to fill main area

## What
- Change the pixiContainer div from `w-[10vw] h-[10vh]` to a div that fills 100% of its parent.
- Wrap VillageNew template in a `div` with `class="w-full h-full relative"` so the canvas and overlays are positioned correctly.
- The PixiJS `resizeTo` option already targets `window`; change it to `pixiContainer.value` so the canvas tracks the actual container element rather than the full browser window.

## Acceptance Criteria
- AC1: Canvas fills the full `<main>` area; resizing keeps it fitted.

## Status
DONE
