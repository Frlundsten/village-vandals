package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_PRODUCTION_PER_HOUR;

import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.village.VillageRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

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
    int producedWood =
        amountProducedSinceLastUpdate(production.getWoodPerHour(), secondsSinceLastUpdate);
    int producedBricks =
        amountProducedSinceLastUpdate(production.getBricksPerHour(), secondsSinceLastUpdate);
    int producedIron =
        amountProducedSinceLastUpdate(production.getIronPerHour(), secondsSinceLastUpdate);
    int producedFood =
        amountProducedSinceLastUpdate(production.getFoodPerHour(), secondsSinceLastUpdate);

    // Update stored amounts
    storage.setWood(storage.getWood() + producedWood);
    storage.setBricks(storage.getBricks() + producedBricks);
    storage.setIron(storage.getIron() + producedIron);
    storage.setFood(storage.getFood() + producedFood);

    storage.setLastUpdate(now);

    repository.save(village);

    return storage;
  }

  private int amountProducedSinceLastUpdate(int productionRate, long secondsSinceLastUpdate) {
    return (int) (productionRate * (secondsSinceLastUpdate / DEFAULT_PRODUCTION_PER_HOUR));
  }
}
