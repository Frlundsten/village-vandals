# Task 2: Loading overlay

## What
- Add a `loading` ref (Boolean, starts `true`).
- Set `loading.value = false` after all assets are loaded and tiles rendered (end of the try block in onMounted).
- Add a `v-if="loading"` overlay div over the canvas: centered text "Loading village…", semi-transparent dark background, absolute-positioned to fill the container.

## Acceptance Criteria
- AC6: A loading overlay is visible during asset loading and hidden afterward.

## Status
DONE
