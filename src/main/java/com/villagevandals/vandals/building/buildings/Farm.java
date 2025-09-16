package com.villagevandals.vandals.building.buildings;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import static com.villagevandals.vandals.building.buildings.BuildingType.FARM;
import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;

@Entity
@DiscriminatorValue("FARM")
public class Farm extends Building {
    @Override
    public int productionPerHour() {
        return DEFAULT_ECONOMICAL_PRODUCTION_RATE *  getLevel();
    }

    public Farm() {
        setType(FARM.name());
    }
}
