package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.BARRACK;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_BASE_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_HEAVY_COST;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;

/**
 * Barrack building allows training of combat units. It does not produce resources.
 */
@Entity
@DiscriminatorValue("BARRACK")
public class Barrack extends Building implements MilitaryBuilding {

  public Barrack() {
    setType(BARRACK.name());
  }

  @Override
  public Map<Resource, Integer> getConstructionCost() {
    return Map.of(Resource.WOOD, 100, Resource.BRICKS, 80, Resource.FOOD, 60, Resource.IRON, 40);
  }

  @Override
  public Map<Resource, Integer> getUpgradeCostAsResourceMap() {
    return Map.of(
        Resource.WOOD, UPGRADE_BASE_COST * nextLevel(),
        Resource.BRICKS, UPGRADE_HEAVY_COST * nextLevel(),
        Resource.FOOD, UPGRADE_BASE_COST * nextLevel(),
        Resource.IRON, UPGRADE_HEAVY_COST * nextLevel());
  }
}
