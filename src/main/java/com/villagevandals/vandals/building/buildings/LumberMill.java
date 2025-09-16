package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.LUMBERMILL;
import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LUMBER_MILL")
public class LumberMill extends Building {
  @Override
  public int productionPerHour() {
    return DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel();
  }

  public LumberMill() {
    setType(LUMBERMILL.name());
  }
}
