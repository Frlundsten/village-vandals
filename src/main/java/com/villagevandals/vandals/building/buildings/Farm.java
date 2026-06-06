package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.FARM;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_BASE_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.UPGRADE_HEAVY_COST;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;

@Entity
@DiscriminatorValue("FARM")
public class Farm extends AbstractEconomicBuilding {
    @Override
    public int productionPerHour() {
        return DEFAULT_ECONOMICAL_PRODUCTION_RATE * getLevel();
    }

    @Override
    public Resource producedResource() {
        return Resource.FOOD;
    }

    public Farm() {
        setType(FARM.name());
    }

    @Override
    public Map<Resource, Integer> getConstructionCost() {
        return Map.of(Resource.WOOD, 60, Resource.FOOD, 40);
    }

    @Override
    public Map<Resource, Integer> getUpgradeCostAsResourceMap() {
        return Map.of(
            Resource.WOOD, UPGRADE_HEAVY_COST * nextLevel(),
            Resource.BRICKS, UPGRADE_BASE_COST * nextLevel(),
            Resource.FOOD, UPGRADE_BASE_COST * nextLevel(),
            Resource.IRON, UPGRADE_BASE_COST * nextLevel());
    }
}
