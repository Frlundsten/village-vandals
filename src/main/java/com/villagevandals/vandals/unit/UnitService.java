package com.villagevandals.vandals.unit;

import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_FOOD_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_IRON_COST;

import com.villagevandals.vandals.building.BuildingRepository;
import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.resource.Resource;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitService {

  private final VillageRepository villageRepository;
  private final BuildingRepository buildingRepository;
  private final UnitRepository unitRepository;
  private final ResourcesService resourcesService;

  public UnitService(
      VillageRepository villageRepository,
      BuildingRepository buildingRepository,
      UnitRepository unitRepository,
      ResourcesService resourcesService) {
    this.villageRepository = villageRepository;
    this.buildingRepository = buildingRepository;
    this.unitRepository = unitRepository;
    this.resourcesService = resourcesService;
  }

  @Transactional
  public TrainResponseDTO trainVandal(long villageId, long buildingId) {
    Village village = villageRepository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found: " + villageId));

    var building = buildingRepository.findById(buildingId)
        .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingId));

    if (!(building instanceof Barrack)) {
      throw new IllegalArgumentException(
          "Building " + buildingId + " is not a Barrack — cannot train units here");
    }

    resourcesService.snapshotCurrentResources(villageId);
    resourcesService.deductResources(villageId, Map.of(Resource.FOOD, VANDAL_FOOD_COST, Resource.IRON, VANDAL_IRON_COST));

    Vandal vandal = new Vandal();
    unitRepository.save(vandal);
    village.getUnits().add(vandal);
    villageRepository.save(village);

    return new TrainResponseDTO(vandal.getUnitType(), vandal.getHp(), vandal.getDamage(), villageId);
  }

  public List<UnitRosterDTO> getRoster(long villageId) {
    Village village = villageRepository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found: " + villageId));

    return village.getUnits().stream()
        .collect(Collectors.groupingBy(VillageUnit::getUnitType, Collectors.counting()))
        .entrySet().stream()
        .map(entry -> {
          String unitType = entry.getKey();
          long count = entry.getValue();
          VillageUnit representative = village.getUnits().stream()
              .filter(u -> u.getUnitType().equals(unitType))
              .findFirst()
              .orElseThrow();
          return new UnitRosterDTO(unitType, count, representative.getHp(), representative.getDamage());
        })
        .toList();
  }
}
