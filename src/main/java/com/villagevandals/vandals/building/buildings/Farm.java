package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.building.buildings.BuildingType.FARM;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

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
}
