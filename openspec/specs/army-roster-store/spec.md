# Spec: Army Roster Store

## Purpose

A shared Pinia store (`useArmyStore`) that holds the player's village unit roster as a reactive ref, providing a single source of truth that `Home.vue`, `ArmyView.vue`, and the training queue can read from and refresh, so completed training orders are reflected across all army-displaying views without independent per-component state.

## Requirements

### Requirement: Shared reactive army roster store
The system SHALL provide a Pinia store (`useArmyStore`) with a `roster` reactive ref and a `refresh(villageId)` action. Calling `refresh` SHALL fetch `GET /unit?villageId=X`, trigger backend lazy promotion of completed training orders, and update `roster` with the returned unit list. All components reading from `armyStore.roster` SHALL reflect the update in the same render cycle.

#### Scenario: Refresh populates roster
- **WHEN** `armyStore.refresh(villageId)` is called and the backend returns unit data
- **THEN** `armyStore.roster` is updated with the returned units
- **AND** all components reading `armyStore.roster` reactively display the new values

#### Scenario: Refresh with empty roster
- **WHEN** `armyStore.refresh(villageId)` is called and the village has no units
- **THEN** `armyStore.roster` is set to an empty array

### Requirement: Army views read from the shared store
`Home.vue` (mini-panel) and `ArmyView.vue` (army tab) SHALL read the unit roster from `useArmyStore` instead of managing independent local refs. Each view SHALL call `armyStore.refresh(villageId)` on mount to ensure the store is populated.

#### Scenario: Home mini-panel shows current roster
- **WHEN** `armyStore.roster` is updated (e.g., by training completion)
- **THEN** the army mini-panel under the Logout button reactively shows the updated unit counts without page refresh

#### Scenario: Army view shows current roster on navigation
- **WHEN** the player navigates to the Army tab
- **THEN** `ArmyView.vue` calls `armyStore.refresh` on mount
- **AND** displays the units currently in `armyStore.roster`

### Requirement: Training completion triggers roster refresh
When the client-side countdown for a training order reaches zero, the system SHALL call `armyStore.refresh(villageId)` to promote completed orders to units on the backend and update the reactive roster. The refresh SHALL be fire-and-forget — errors SHALL be caught and discarded without affecting the queue display.

#### Scenario: Roster updates when countdown expires
- **WHEN** the training queue countdown for the first order reaches zero
- **THEN** `armyStore.refresh(villageId)` is called exactly once
- **AND** `armyStore.roster` is updated with the newly promoted unit
- **AND** both `Home.vue` mini-panel and `ArmyView.vue` reflect the new count

#### Scenario: Roster refresh error does not break the queue
- **WHEN** `armyStore.refresh` throws a network error after an order expires
- **THEN** the queue display is unaffected
- **AND** `armyStore.roster` retains its previous value
