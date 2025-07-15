package com.villagevandals.vandals.service.village;

import com.villagevandals.vandals.controller.building.ConstructBuildingDTO;
import com.villagevandals.vandals.model.domain.buildings.Building;
import com.villagevandals.vandals.model.domain.buildings.LumberMill;
import com.villagevandals.vandals.repository.BuildingRepository;
import com.villagevandals.vandals.repository.village.ConstructionSite;
import com.villagevandals.vandals.repository.village.ConstructionSiteRepository;
import com.villagevandals.vandals.repository.village.VillageRepository;
import com.villagevandals.vandals.service.ResourcesService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuildingService {

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
  public void constructBuilding(ConstructBuildingDTO dto) {
    var optionalSite =
        constructionSiteRepository.findByIdAndVillageId(dto.constructionSiteId(), dto.villageId());
    var site =
        optionalSite.orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Construction site not found for given ID and village"));
    var mill = new LumberMill();
    site.setBuilding(mill);
    constructionSiteRepository.save(site);

    resourcesService.updateProduction(mill, dto.villageId());
  }

  public List<Building> getAllBuildingsByVillageId(Long villageId, String username) {
    List<ConstructionSite> siteInVillageId = constructionSiteRepository.findByVillageId(villageId);
    if (!siteInVillageId.getFirst().getVillage().getOwner().getUsername().equals(username)) {
      throw new IllegalArgumentException("Not valid owner");
    }

    return siteInVillageId.stream().map(ConstructionSite::getBuilding).toList();
  }
}
