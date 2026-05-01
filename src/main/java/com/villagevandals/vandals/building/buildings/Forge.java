package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.FORGE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;

@Entity
@DiscriminatorValue("FORGE")
public class Forge extends AbstractEconomicBuilding {
  @Override
  public int productionPerHour() {
    return DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel();
  }

  @Override
  public Resource producedResource() {
    return Resource.IRON;
  }

  public Forge() {
    setType(FORGE.name());
  }

  @Override
  public Map<Resource, Integer> getConstructionCost() {
    return Map.of(Resource.WOOD, 80, Resource.BRICKS, 60, Resource.FOOD, 40);
  }
}
