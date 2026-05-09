# Task 2 — Implement the fix in VillageNew.vue

1. Add `let dragInitiatedOnCanvas = false` alongside the other drag state variables.
2. In `pointerDownFn`: set `dragInitiatedOnCanvas = true`.
3. In `pointerUpFn`: set `dragInitiatedOnCanvas = false`.
4. In `globalpointermove` handler: add early return `if (!dragInitiatedOnCanvas) return`
   as the very first line.
