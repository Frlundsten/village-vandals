package com.villagevandals.vandals.building;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/building")
public class BuildingController {

  private static final Logger LOG = LoggerFactory.getLogger(BuildingController.class);

  BuildingService buildingService;

  public BuildingController(BuildingService buildingService) {
    this.buildingService = buildingService;
  }

  @GetMapping
  public List<Building> getExistingBuildings(@RequestParam Long villageId, Principal principal) {
    String username = principal.getName();
    return buildingService.getAllBuildingsByVillageId(villageId, username);
  }

  @PostMapping
  public ResponseEntity<?> createBuilding(@RequestBody ConstructionRequestDTO dto) {
    LOG.debug("Got a request to construct: {}", dto);
    try {
      buildingService.constructBuilding(dto);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
        LOG.debug(e.getMessage());
      return ResponseEntity.badRequest().body("Unable to construct building");
    }
  }

  @GetMapping("/available")
  public List<Building> getAvailableBuildings(@RequestParam Long villageId, Principal principal) {
    try {
      List<Building> result = buildingService.getAvailableBuildings(villageId, principal.getName());
      LOG.info("Available buildings: {}", result);
      return result;
    } catch (Exception e) {
      return List.of();
    }
  }
}
