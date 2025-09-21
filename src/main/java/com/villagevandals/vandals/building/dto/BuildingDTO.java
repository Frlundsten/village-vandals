package com.villagevandals.vandals.building.dto;

import com.villagevandals.vandals.building.buildings.Building;

public record BuildingDTO(Long constructionSiteId, Long buildingId, String type, int level) {
  public static BuildingDTO fromEntity(Long constructionSiteId, Building building) {
    return new BuildingDTO(
        constructionSiteId, building.getId(), building.getType(), building.getLevel());
  }
}
