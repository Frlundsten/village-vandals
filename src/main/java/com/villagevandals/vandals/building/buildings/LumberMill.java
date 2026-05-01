package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.LUMBERMILL;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;

@Entity
@DiscriminatorValue("LUMBER_MILL")
public class LumberMill extends AbstractEconomicBuilding {
  @Override
  public int productionPerHour() {
    return DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel();
  }

  @Override
  public Resource producedResource() {
    return Resource.WOOD;
  }

  public LumberMill() {
    setType(LUMBERMILL.name());
  }

  @Override
  public Map<Resource, Integer> getConstructionCost() {
    return Map.of(Resource.FOOD, 50, Resource.BRICKS, 60);
  }
}
