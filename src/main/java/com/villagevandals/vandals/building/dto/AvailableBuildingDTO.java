package com.villagevandals.vandals.building.dto;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.resource.Resource;
import java.util.Map;
import java.util.stream.Collectors;

public record AvailableBuildingDTO(String type, Map<String, Integer> constructionCost) {

  public static AvailableBuildingDTO fromEntity(Building building) {
    Map<String, Integer> cost = building.getConstructionCost().entrySet().stream()
        .collect(Collectors.toMap(
            e -> e.getKey().name().toLowerCase(),
            Map.Entry::getValue));
    return new AvailableBuildingDTO(building.getType(), cost);
  }
}
