package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.BARRACK;

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
}
