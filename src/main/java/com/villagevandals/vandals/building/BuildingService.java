package com.villagevandals.vandals.building;

import com.villagevandals.vandals.building.buildings.AbstractEconomicBuilding;
import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.building.buildings.Brickyard;
import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.buildings.EconomicBuilding;
import com.villagevandals.vandals.building.buildings.EconomicProduction;
import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.building.buildings.Forge;
import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import com.villagevandals.vandals.building.dto.UpgradeRequestDTO;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  /**
   * Constructs a building on an empty construction site.
   * Snapshots current resources, validates the site is unoccupied, deducts the construction cost,
   * persists the building, and updates village production if the building is economic.
   *
   * @throws IllegalArgumentException if the village or site is not found, the site is already
   *     occupied, or resources are insufficient
   */
  @Transactional
  public void constructBuilding(ConstructionRequestDTO dto) {

    Village village = getVillageFromDto(dto);

    ConstructionSite unpopulatedSite =
        getUnpopulatedSite(dto.constructionSiteId(), dto.villageId());

    Building building = getBuildingByType(dto.type());
    unpopulatedSite.setBuilding(building);
    unpopulatedSite.setVillage(village);
    resourcesService.snapshotCurrentResources(dto.villageId());
    resourcesService.deductResources(dto.villageId(), building.getConstructionCost());

    constructionSiteRepository.save(unpopulatedSite);
    if (building instanceof EconomicProduction eco) {
      resourcesService.updateProduction(eco, dto.villageId());
    }
  }

  private Building getBuildingByType(String type) {
    return switch (type) {
      case "FARM" -> new Farm();
      case "LUMBERMILL" -> new LumberMill();
      case "BARRACK" -> new Barrack();
      case "BRICKYARD" -> new Brickyard();
      case "FORGE" -> new Forge();
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  private ConstructionSite getUnpopulatedSite(long constructionSiteId, long villageId) {
    LOG.debug("Get unpopulated site for construction site with id {}", constructionSiteId);
    ConstructionSite site =
        constructionSiteRepository
            .findByIdAndVillageId(constructionSiteId, villageId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Something went wrong when fetching unpopulated site"));

    if (site.getBuilding() != null) {
      throw new IllegalArgumentException("A building already exists on this site");
    }
    return site;
  }

  private Village getVillageFromDto(ConstructionRequestDTO dto) {
    Optional<Village> village = villageRepository.findById(dto.villageId());
    return village.orElseThrow(() -> new IllegalArgumentException("Village Not Found"));
  }

  /**
   * Returns all constructed buildings for the village keyed by construction site ID.
   * Empty sites are excluded. Validates that {@code username} owns the village.
   * Returns an empty map if the village has no buildings yet.
   */
  public Map<Long, Building> getAllBuildingsByVillageId(Long villageId, String username) {
    try {
      List<ConstructionSite> siteInVillageId =
          constructionSiteRepository.findByVillageId(villageId).stream()
              .filter(Objects::nonNull)
              .filter(site -> site.getBuilding() != null)
              .toList();


      if (siteInVillageId.isEmpty()) {
        LOG.error("No buildings found for village {}", villageId);
        return Map.of();
      }

      validateOwner(siteInVillageId.getFirst(), username);
      return siteInVillageId.stream()
          .collect(Collectors.toMap(ConstructionSite::getVillageSiteId, ConstructionSite::getBuilding));

    } catch (Exception e) {
      LOG.error("Something went wrong: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the full catalogue of building types that can be constructed.
   * The list is static for now — village state and tech level are not yet considered.
   */
  public List<Building> getAvailableBuildings(long villageId, String userName) {
    return List.of(new LumberMill(), new Farm(), new Barrack(), new Brickyard(), new Forge());
  }

  /**
   * Upgrades the building at the given construction site and updates the village's production
   * rate by the incremental delta. Only the authenticated owner may upgrade buildings.
   *
   * @throws IllegalArgumentException if the site is not found or {@code username} is not the owner
   * @throws IllegalStateException if there is no building at the site
   */
  @Transactional
  public Building upgradeBuilding(UpgradeRequestDTO dto, String username) {
    ConstructionSite site = getConstructionSite(dto.constructionSiteId(), dto.villageId());

    validateOwner(site, username);

    Building building = getBuildingToUpgrade(site);

    int delta = upgradeAndGetProductionDelta(building);

    buildingRepository.save(building);

    if (building instanceof EconomicProduction eco) {
      resourcesService.updateProductionDelta(eco, dto.villageId(), delta);
    }
    return building;
  }

  private ConstructionSite getConstructionSite(long constructionSiteId, long villageId) {
    return constructionSiteRepository
        .findByIdAndVillageId(constructionSiteId, villageId)
        .orElseThrow(() -> new IllegalArgumentException("Construction site not found"));
  }

  private void validateOwner(ConstructionSite site, String username) {
    var ownerUsername =
        Optional.ofNullable(site)
            .map(ConstructionSite::getVillage)
            .map(Village::getOwner)
            .map(User::getUsername)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Construction site has no valid village or owner"));

    if (!username.equals(ownerUsername)) {
      throw new IllegalArgumentException("Not valid owner");
    }
  }

  private Building getBuildingToUpgrade(ConstructionSite site) {
    return Optional.ofNullable(site.getBuilding())
        .orElseThrow(() -> new IllegalStateException("No building to upgrade at this site"));
  }

  private int upgradeAndGetProductionDelta(Building building) {
    if (building instanceof AbstractEconomicBuilding eco) {
      return upgradeEconomicBuilding(eco);
    } else {
      // Military or non-economic buildings upgrading does not affect resource production
      building.upgrade();
    }
    return 0;
  }

  private int upgradeEconomicBuilding(AbstractEconomicBuilding eco) {
    int before = eco.productionPerHour();
    eco.upgrade();
    int after = eco.productionPerHour();
    return after - before;
  }
}
