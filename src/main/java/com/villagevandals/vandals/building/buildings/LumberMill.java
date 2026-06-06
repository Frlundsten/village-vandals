package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.LUMBERMILL;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_BASE_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_HEAVY_COST;

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

  @Override
  public Map<Resource, Integer> getUpgradeCostAsResourceMap() {
    return Map.of(
        Resource.WOOD, UPGRADE_BASE_COST * nextLevel(),
        Resource.BRICKS, UPGRADE_HEAVY_COST * nextLevel(),
        Resource.FOOD, UPGRADE_BASE_COST * nextLevel(),
        Resource.IRON, UPGRADE_BASE_COST * nextLevel());
  }
}
