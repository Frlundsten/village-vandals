package com.villagevandals.vandals.building.dto;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.buildings.EconomicProduction;
import java.util.Map;

public record BuildingDTO(
    Long constructionSiteId,
    Long buildingId,
    String type,
    int level,
    Map<String, Integer> upgradeCost,
    Integer productionPerHour) {

  public static BuildingDTO fromEntity(Long constructionSiteId, Building building) {
    Integer production =
        building instanceof EconomicProduction eco ? eco.productionPerHour() : null;
    return new BuildingDTO(
        constructionSiteId,
        building.getId(),
        building.getType(),
        building.getLevel(),
        building.getUpgradeCost(),
        production);
  }
}
