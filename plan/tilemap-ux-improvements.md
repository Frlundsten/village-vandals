# Spec: Tilemap UX Improvements

## Requirements

1. **Full-area canvas** — The PixiJS canvas container is currently `w-[10vw] h-[10vh]`. It must fill the entire `<main>` area (full width and height minus header + sidebar from Home.vue).
2. **Instant drag / click discrimination** — Replace the 500 ms timer with a movement-threshold: dragging activates once the pointer moves > 5 px from its start point; releasing without crossing that threshold counts as a click.
3. **Mouse-wheel zoom** — Scroll to zoom in/out, clamped to 0.3×–3×, zooming toward the pointer position.
4. **Hover highlight on interactive tiles** — Construction-site tile sprites brighten on `pointerover` and restore on `pointerout`.
5. **Map bounds clamping** — After panning or zooming, the map cannot be dragged so far that less than 20 % of its bounding box is on-screen in either axis.
6. **Loading overlay** — A Vue overlay ("Loading village…") is shown while assets load and hidden once `onMounted` finishes.

## Acceptance Criteria

- AC1: Canvas fills the full `<main>` area; resizing keeps it fitted.
- AC2: Clicking a construction-site tile opens BuildingMenu; dragging does not trigger it.
- AC3: Mouse-wheel zooms the map toward the pointer; scale clamped to 0.3–3.
- AC4: Hovering a construction-site tile changes its tint; leaving restores it.
- AC5: Panning cannot move the map so that < 20 % of its bounding box is on-screen.
- AC6: A loading overlay is visible during asset loading and hidden afterward.

## Architecture Impact

- `VillageNew.vue` only — no backend changes, no new endpoints.
- Template: wrapper `div` filling parent, loading overlay `div`, canvas container `div` sized to fill available space.
- Script: replaces timer-based drag with movement-threshold logic; adds wheel listener on `app.stage` for zoom; adds `tint` hover on interactive tile sprites; adds bounds-clamping helper; adds `loading` ref.
- Uses PixiJS v8 APIs: `eventMode = 'static'`, `globalpointermove`, `wheel` event, `tint`.

## Out of Scope

- Pinch-to-zoom (touch).
- Animated loading spinner.
- Minimap or re-center button.
- Changes to BuildingMenu, BuildingUpgradeCard, or any other component.
- Backend changes.
