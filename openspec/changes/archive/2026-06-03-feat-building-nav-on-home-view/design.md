## Context

The home layout (`Home.vue`) has a sidebar with named navigation items. "Village" and "World Map" use `<RouterLink>` and have working routes. "Buildings", "Army", "Reports", and "Messages" use `@click="goTo()"` stubs that log to the console. The `<RouterView>` in the main content area renders whatever the active child route is. Adding a `/buildings` child route and replacing the Buildings `<a>` with a `<RouterLink>` is the minimal, consistent approach that matches how Village navigation already works.

The existing `GET /building?villageId` endpoint returns `BuildingDTO[]` — each item has `constructionSiteId`, `buildingId`, `type`, `level`, `upgradeCost` (map of resource → cost), and `productionPerHour` (null for Barracks). `POST /building/upgrade` takes `{ villageId, constructionSiteId }`.

`useResourceStore` already exposes `food`, `wood`, `bricks`, `iron` as reactive refs. Affordability is a simple `Object.entries(upgradeCost).every(([r, cost]) => resourceStore[r] >= cost)`.

## Goals / Non-Goals

**Goals:**
- Display all village buildings in a panel accessible from the sidebar.
- Show level, icon, type name, and an Upgrade button per row.
- Upgrade button disabled (not hidden) when the player cannot afford the next level.
- Upgrade triggers the API, refreshes resources and building list, shows feedback.

**Non-Goals:**
- Barracks unit training — that's already in `BuildingUpgradeCard.vue`; the buildings panel focuses on the overview and economic building upgrades only. Barracks will show their level and icon but with a disabled upgrade button (Barracks upgrade not in scope).
- Construction of new buildings (handled on the village map).
- Animations or complex transitions.

## Decisions

### 1. Inline panel, not a modal

`BuildingUpgradeCard.vue` is a modal dialog triggered from the village tile map. The Buildings panel is a persistent view in the main content area — it replaces `<RouterView>` content when the Buildings tab is active. This is consistent with how Village and World Map work.

### 2. Route: `/buildings` as a sibling child of home

```
/                              ← home layout (Home.vue)
  /village/:villageId          ← village map
  /village                     ← village map (no ID)
  /map                         ← world map
  /buildings                   ← NEW: buildings panel
```

`villageId` is read inside the component from `localStorage.getItem('villageId')` or from `useResourceStore`/`Home.vue` state — the same pattern used in `Home.vue`'s `loadUserData()`.

### 3. Affordability: computed per building, live

Each building row computes `canAfford` reactively from the resource store. No manual refresh needed — the store is already kept live after upgrades via `resourceStore.refresh()`.

### 4. Visual layout: table rows with DaisyUI

```
┌────────────────────────────────────────────────────┐
│  Buildings                                         │
├──────┬──────────┬────────────────┬────────────────┤
│ Lv   │  Icon    │  Name          │  Action        │
├──────┼──────────┼────────────────┼────────────────┤
│  2   │ 🏗 img   │ LUMBERMILL     │ [⬆ Upgrade]   │  ← btn-success
│  1   │ 🏗 img   │ FARM           │ [⬆ Upgrade]   │  ← btn-disabled
│  3   │ 🏗 img   │ FORGE          │ [⬆ Upgrade]   │  ← btn-success
│  1   │ 🏗 img   │ BARRACK        │ [⬆ Upgrade]   │  ← btn-disabled (always)
└──────┴──────────┴────────────────┴────────────────┘
```

Uses a DaisyUI table (`table table-zebra`) inside a card. Upgrade button uses `btn-success` when affordable, muted/`btn-disabled` when not. Small inline feedback ("Upgrading…", "Done ✓", error message) shown below the button row.

### 5. `villageId` sourcing

The panel reads `villageId` from `localStorage.getItem('villageId')` (parsed as a number), same as `VillageNew.vue` and `Home.vue`. No prop drilling needed since the panel is a route-level component.

## Risks / Trade-offs

- **Stale building list**: After an upgrade the list is re-fetched from the API, keeping it in sync. Resources are refreshed via `resourceStore.refresh()`.
- **No loading skeleton**: A simple "Loading…" text is sufficient; a skeleton is not in scope.
- **Barracks always disabled for upgrade**: Acceptable for now — Barrack upgrades can be added when the Army tab is built.
