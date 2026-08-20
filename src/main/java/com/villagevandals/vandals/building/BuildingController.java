package com.villagevandals.vandals.building;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.dto.AvailableBuildingDTO;
import com.villagevandals.vandals.building.dto.BuildingDTO;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import com.villagevandals.vandals.building.dto.UpgradeRequestDTO;
import com.villagevandals.vandals.web.Message;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Failures are translated by {@link com.villagevandals.vandals.web.GlobalExceptionHandler}: an
 * ownership violation becomes {@code 403}, an invalid argument {@code 400}, and an operation that
 * does not apply to the current state {@code 409}. Handlers therefore do not catch domain
 * exceptions themselves — doing so previously reported every failure, authorization included, as a
 * generic {@code 400 "Unable to ..."} that hid the real reason from the client.
 */
@RestController
@RequestMapping("/building")
public class BuildingController {

  private static final Logger LOG = LoggerFactory.getLogger(BuildingController.class);

  BuildingService buildingService;

  public BuildingController(BuildingService buildingService) {
    this.buildingService = buildingService;
  }

  @GetMapping
  public List<BuildingDTO> getExistingBuildings(@RequestParam Long villageId, Principal principal) {
    String username = principal.getName();
    LOG.debug("Got request to get existing buildings for user {}", username);

    return buildingService.getAllBuildingsByVillageId(villageId, username).entrySet().stream()
        .map(k -> BuildingDTO.fromEntity(k.getKey(), k.getValue()))
        .toList();
  }

  @PostMapping
  public ResponseEntity<?> createBuilding(
      @RequestBody ConstructionRequestDTO dto, Principal principal) {
    LOG.debug("Got a request to construct: {}", dto);
    buildingService.constructBuilding(dto, principal.getName());
    return ResponseEntity.ok(Message.of("Constructed building " + dto.type() + " successfully"));
  }

  @GetMapping("/available")
  public List<AvailableBuildingDTO> getAvailableBuildings(
      @RequestParam Long villageId, Principal principal) {
    return buildingService.getAvailableBuildings(villageId, principal.getName()).stream()
        .map(AvailableBuildingDTO::fromEntity)
        .toList();
  }

  @PostMapping("/upgrade")
  public ResponseEntity<?> upgradeBuilding(
      @RequestBody UpgradeRequestDTO dto, Principal principal) {
    Building upgraded = buildingService.upgradeBuilding(dto, principal.getName());
    return ResponseEntity.ok(BuildingDTO.fromEntity(dto.constructionSiteId(), upgraded));
  }

  /**
   * Demolishes the building on the given site, freeing the site for reuse.
   *
   * @param villageId the village owning the site
   * @param constructionSiteId the site to clear
   * @param principal the authenticated caller, who must own the village
   */
  @DeleteMapping
  public ResponseEntity<?> deleteBuilding(
      @RequestParam Long villageId, @RequestParam Long constructionSiteId, Principal principal) {
    LOG.debug(
        "Got a request to demolish building on site {} in village {}", constructionSiteId, villageId);
    buildingService.deleteBuilding(villageId, constructionSiteId, principal.getName());
    return ResponseEntity.ok(Message.of("Demolished building successfully"));
  }
}
