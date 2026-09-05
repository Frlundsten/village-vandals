# Spec: User Account Provisioning

## Purpose

Creating an account and its starter village is a single unit of work, so a failed registration never leaves a claimed world tile marked occupied with no village on it.

---

## Requirements

### Requirement: Account creation and its starter village are atomic
Creating a user SHALL claim a world tile, persist the user, persist the starter village, and create the village's construction sites as a single unit of work. If any step fails, none of them SHALL persist — in particular a claimed world tile SHALL NOT remain marked occupied without a village on it.

#### Scenario: A failure after the tile is claimed rolls the claim back
- **GIVEN** a free world tile is selected and marked occupied during registration
- **WHEN** persisting the user subsequently fails
- **THEN** the tile claim is rolled back and the tile remains available for the next registration

#### Scenario: Successful registration claims the tile and creates the village
- **WHEN** a new Keycloak user is provisioned
- **THEN** the user, the starter village on the claimed tile, and its construction sites are all persisted together
