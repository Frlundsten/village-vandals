package com.villagevandals.vandals.building;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuildingService {

  private static final Logger LOG = LoggerFactory.getLogger(BuildingService.class);

  VillageRepository villageRepository;
  ConstructionSiteRepository constructionSiteRepository;
  BuildingRepository buildingRepository;
  ResourcesService resourcesService;

  public BuildingService(
      ResourcesService resourcesService,
      VillageRepository villageRepository,
      ConstructionSiteRepository constructionSiteRepository,
      BuildingRepository buildingRepository) {
    this.resourcesService = resourcesService;
    this.buildingRepository = buildingRepository;
    this.villageRepository = villageRepository;
    this.constructionSiteRepository = constructionSiteRepository;
  }

  @Transactional
  public void constructBuilding(ConstructionRequestDTO dto) {

    Village village = getVillageFromDto(dto);

    ConstructionSite unpopulatedSite =
        getUnpopulatedSite(dto.constructionSiteId(), dto.villageId());

    Building building = getBuildingByType(dto.type());
    unpopulatedSite.setBuilding(building);
    unpopulatedSite.setVillage(village);
    constructionSiteRepository.save(unpopulatedSite);
    resourcesService.updateProduction(building, dto.villageId());
  }

  private Building getBuildingByType(String type) {
    return switch (type) {
      case "FARM" -> new Farm();
      case "LUMBERMILL" -> new LumberMill();
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  private ConstructionSite getUnpopulatedSite(long constructionSiteId, long villageId) {
    Optional<ConstructionSite> optionalSite =
        constructionSiteRepository.findByIdAndVillageId(constructionSiteId, villageId);
    if (optionalSite.isPresent()) {
      if (optionalSite.get().getBuilding() != null) {
        throw new IllegalArgumentException(
            "Building Already Exists " + optionalSite.get().getBuilding().getType());
      } else {
        return optionalSite.get();
      }
    } else {
      LOG.error(
          "Construction site not found for given ID and village : village {}, site {}",
          villageId,
          constructionSiteId);
      throw new IllegalArgumentException();
    }
  }

  private Village getVillageFromDto(ConstructionRequestDTO dto) {
    Optional<Village> village = villageRepository.findById(dto.villageId());
    return village.orElseThrow(() -> new IllegalArgumentException("Village Not Found"));
  }

  public Map<Long, Building> getAllBuildingsByVillageId(Long villageId, String username) {
    try {
      List<ConstructionSite> siteInVillageId =
          constructionSiteRepository.findByVillageId(villageId).stream()
              .filter(site -> site.getBuilding() != null)
              .toList();
      if (siteInVillageId == null || siteInVillageId.isEmpty()) {
        throw new IllegalArgumentException("Village Not Found");
      }
      if (!siteInVillageId.getFirst().getVillage().getOwner().getUsername().equals(username)) {
        throw new IllegalArgumentException("Not valid owner");
      }
      return siteInVillageId.stream()
          .collect(Collectors.toMap(ConstructionSite::getId, ConstructionSite::getBuilding));

    } catch (Exception e) {
      LOG.error("Something went wrong: {}", e);
      throw new RuntimeException(e);
    }
  }

  public List<Building> getAvailableBuildings(long villageId, String userName) {
    return List.of(new LumberMill(), new Farm());
  }
}
