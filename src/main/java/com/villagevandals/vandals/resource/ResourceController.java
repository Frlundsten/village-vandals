package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.resource.Resource.*;

import java.util.HashMap;
import java.util.Map;
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
  Map<String, Integer> handleResourceAction(@RequestParam("villageId") long villageId) {
    ResourceStorage storage = resourcesService.getCurrentResourceStorage(villageId);
    Map<String, Integer> resources = new HashMap<>();
    resources.put("wood", storage.get(WOOD));
    resources.put("iron", storage.get(IRON));
    resources.put("bricks", storage.get(BRICKS));
    resources.put("food", storage.get(FOOD));
    return resources;
  }
}
