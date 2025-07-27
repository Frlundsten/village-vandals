package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.resource.Resource.BRICKS;
import static com.villagevandals.vandals.resource.Resource.FOOD;
import static com.villagevandals.vandals.resource.Resource.IRON;
import static com.villagevandals.vandals.resource.Resource.WOOD;
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
    storage.set(WOOD, updateAmount(storage.get(WOOD), producedWood));
    storage.set(BRICKS, updateAmount(storage.get(BRICKS), producedBricks));
    storage.set(IRON, updateAmount(storage.get(IRON), producedIron));
    storage.set(FOOD, updateAmount(storage.get(FOOD), producedFood));
    storage.setLastUpdate(now);

    repository.save(village);

    return storage;
  }

  private int updateAmount(int storedAmount, int producedAmount) {
    return storedAmount + producedAmount;
  }

  private int amountProducedSinceLastUpdate(int productionRate, long secondsSinceLastUpdate) {
    return (int) (productionRate * (secondsSinceLastUpdate / (double) DEFAULT_PRODUCTION_PER_HOUR)); // Cast to double or else you will get 0!!
  }
}
