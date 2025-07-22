package com.villagevandals.vandals.resource;

import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.village.VillageRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ResourcesService {
  VillageRepository repository;

  public ResourcesService(VillageRepository repository) {
    this.repository = repository;
  }

  public void updateProduction(LumberMill mill, long villageId) {
    var optionalVillage = repository.findById(villageId);
    if (optionalVillage.isPresent()) {
      var villageResource = optionalVillage.get();
      var production = villageResource.getProduction();
      production.setWoodPerHour(production.getWoodPerHour() + mill.productionPerHour());
      repository.save(villageResource);
    } else {
      throw new IllegalStateException("No such village...");
    }
  }

  public ResourceStorage handleUserAction(long villageId) {
    var villageOpt = repository.findById(villageId);
    var village = villageOpt.orElseThrow(() -> new IllegalArgumentException("Village not found"));

    var storage = village.getStorage();
    var production = village.getProduction();

    Instant now = Instant.now();
    long secondsSinceLastUpdate = Duration.between(storage.getLastUpdate(), now).getSeconds();

    // Calculate produced resources since last update
    int producedWood = (int) (production.getWoodPerHour() * (secondsSinceLastUpdate / 3600.0));
    int producedClay = (int) (production.getClayPerHour() * (secondsSinceLastUpdate / 3600.0));
    int producedIron = (int) (production.getIronPerHour() * (secondsSinceLastUpdate / 3600.0));
    int producedCrop = (int) (production.getCropPerHour() * (secondsSinceLastUpdate / 3600.0));

    // Update stored amounts
    storage.setWood(storage.getWood() + producedWood);
    storage.setStone(storage.getStone() + producedClay); // assuming clay is stored as stone?
    storage.setIron(storage.getIron() + producedIron);
    storage.setCrop(storage.getCrop() + producedCrop);

    storage.setLastUpdate(now);

    repository.save(village);

    return storage;
  }
}
