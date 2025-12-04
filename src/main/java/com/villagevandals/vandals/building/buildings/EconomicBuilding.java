package com.villagevandals.vandals.building.buildings;

import com.villagevandals.vandals.resource.Resource;

/**
 * Represents a building that produces a specific economic resource per hour.
 * Use AbstractEconomicBuilding
 */
@Deprecated
public interface EconomicBuilding extends EconomicProduction {
    /**
     * The resource this building produces.
     */
    Resource producedResource();
}
