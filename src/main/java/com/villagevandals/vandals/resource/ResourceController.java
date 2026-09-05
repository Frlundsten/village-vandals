package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.resource.Resource.*;

import com.villagevandals.vandals.village.VillageOwnershipService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/refresh")
public class ResourceController {

  ResourcesService resourcesService;
  VillageOwnershipService villageOwnershipService;

  public ResourceController(
      ResourcesService resourcesService, VillageOwnershipService villageOwnershipService) {
    this.resourcesService = resourcesService;
    this.villageOwnershipService = villageOwnershipService;
  }

  /**
   * Returns the village's current resource amounts and per-hour rates, snapshotting production up
   * to now as a side effect. Only the village's owner may call it — the snapshot writes to the
   * village row, so an unauthorized call would both leak and mutate someone else's state.
   */
  @GetMapping
  ResourceStorageResponse handleResourceAction(
      @RequestParam("villageId") long villageId, Principal principal) {
    villageOwnershipService.requireOwner(villageId, principal.getName());

    ResourceStorage storage = resourcesService.refreshAndPersist(villageId);
    ResourceProduction production = resourcesService.getProduction(villageId);
    return new ResourceStorageResponse(
        storage.get(FOOD),
        storage.get(WOOD),
        storage.get(BRICKS),
        storage.get(IRON),
        production.getFoodPerHour(),
        production.getWoodPerHour(),
        production.getBricksPerHour(),
        production.getIronPerHour());
  }
}
