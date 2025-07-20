package com.villagevandals.vandals.model.domain.buildings;

import static com.villagevandals.vandals.service.util.GameDefaults.DEFAULT_LUMBERMILL_PRODUCTION_RATE;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LUMBER_MILL")
public class LumberMill extends Building {
  @Override
  public int productionPerHour() {
    return DEFAULT_LUMBERMILL_PRODUCTION_RATE * getLevel();
  }
}
