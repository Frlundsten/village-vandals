package com.villagevandals.vandals.resource;

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
  ResourceStorage handleResourceAction(@RequestParam("villageId") long villageId) {
    return resourcesService.handleUserAction(villageId);
  }
}
