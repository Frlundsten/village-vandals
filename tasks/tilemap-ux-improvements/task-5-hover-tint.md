# Task 5: Hover highlight on construction-site tiles

## What
- In `setInteractiveSpriteTile`, add `pointerover` and `pointerout` listeners.
- On `pointerover`: set `sprite.tint = 0xddddff` (light blue-white brightening).
- On `pointerout`: set `sprite.tint = 0xffffff` (restore to normal).
- Use `eventMode = 'static'` (already set via `sprite.interactive = true` alias, but update to explicit `eventMode` for v8 clarity).

## Acceptance Criteria
- AC4: Hovering a construction-site tile changes its tint; leaving restores it.

## Status
DONE
