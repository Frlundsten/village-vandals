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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResourcesService {
  private static final Logger LOG = LoggerFactory.getLogger(ResourcesService.class);

  VillageRepository repository;

  public ResourcesService(VillageRepository repository) {
    this.repository = repository;
  }

  public void updateProduction(LumberMill mill, long villageId) {
    repository.increaseWoodProduction(villageId, mill.productionPerHour());
  }

  public ResourceStorage getCurrentResourceStorage(long villageId) {

    LOG.debug("Calculating storage resources since last time...");

    var villageOpt = repository.findById(villageId);
    LOG.debug("Find village by id {}", villageId);
    var village = villageOpt.orElseThrow(() -> new IllegalArgumentException("Village not found"));

    var storage = village.getStorage();
    var production = village.getProduction();

    LOG.debug("Storage was last updated : {}", storage.getLastUpdate());

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

    ResourceStorage storageCopy = new ResourceStorage();
    storageCopy.set(WOOD, updateAmount(storage.get(WOOD), producedWood));
    storageCopy.set(BRICKS, updateAmount(storage.get(BRICKS), producedBricks));
    storageCopy.set(IRON, updateAmount(storage.get(IRON), producedIron));
    storageCopy.set(FOOD, updateAmount(storage.get(FOOD), producedFood));
    storageCopy.setLastUpdate(now);

    return storageCopy;
  }

  private int updateAmount(int storedAmount, int producedAmount) {
    return storedAmount + producedAmount;
  }

  private int amountProducedSinceLastUpdate(int productionRate, long secondsSinceLastUpdate) {
    return (int)
        (productionRate
            * (secondsSinceLastUpdate
                / (double) DEFAULT_PRODUCTION_PER_HOUR)); // Cast to double or else you will get 0!!
  }
}
