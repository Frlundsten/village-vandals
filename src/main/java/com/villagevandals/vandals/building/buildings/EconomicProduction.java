package com.villagevandals.vandals.building.buildings;

import com.villagevandals.vandals.resource.Resource;

public interface EconomicProduction {
    int productionPerHour();
    Resource producedResource();
}
