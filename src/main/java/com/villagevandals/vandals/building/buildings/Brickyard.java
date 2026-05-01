package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.BRICKYARD;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;

@Entity
@DiscriminatorValue("BRICKYARD")
public class Brickyard extends AbstractEconomicBuilding {
  @Override
  public int productionPerHour() {
    return DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel();
  }

  @Override
  public Resource producedResource() {
    return Resource.BRICKS;
  }

  public Brickyard() {
    setType(BRICKYARD.name());
  }

  @Override
  public Map<Resource, Integer> getConstructionCost() {
    return Map.of(Resource.WOOD, 70, Resource.FOOD, 50);
  }
}
