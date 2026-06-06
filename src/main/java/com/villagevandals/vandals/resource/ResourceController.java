package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.resource.Resource.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resources/refresh")
public class ResourceController {

  ResourcesService resourcesService;

  public ResourceController(ResourcesService resourcesService) {
    this.resourcesService = resourcesService;
  }

  @GetMapping
  ResourceStorageResponse handleResourceAction(@RequestParam("villageId") long villageId) {
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
