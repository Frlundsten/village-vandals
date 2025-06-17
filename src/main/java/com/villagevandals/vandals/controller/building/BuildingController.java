package com.villagevandals.vandals.controller.building;

import com.villagevandals.vandals.model.domain.buildings.Building;
import com.villagevandals.vandals.service.village.BuildingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/building")
public class BuildingController {

  BuildingService buildingService;

  public BuildingController(BuildingService buildingService) {
    this.buildingService = buildingService;
  }

  @PostMapping
  public ResponseEntity<Building> createBuilding(@RequestBody ConstructBuildingDTO dto) {
    try {
      buildingService.constructBuilding(dto);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
