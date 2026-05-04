# Task 4 — Update VillageNew.vue to write to useResourceStore after build/upgrade

## Goal
Replace `currentResources` local ref with `useResourceStore`.
Call `store.refresh(villageId)` after `constructBuilding` and after `upgradeBuilding`
so the header bar in `Home.vue` auto-updates via the shared store.

## Files changed
- `frontend/src/components/VillageNew.vue`

## Status: TODO
