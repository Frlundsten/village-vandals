package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.BARRACK;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Barrack building allows training of combat units. It does not produce resources.
 */
@Entity
@DiscriminatorValue("BARRACK")
public class Barrack extends Building implements MilitaryBuilding {

  public Barrack() {
    setType(BARRACK.name());
  }
}
