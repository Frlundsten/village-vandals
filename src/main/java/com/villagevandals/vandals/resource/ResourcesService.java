package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_PRODUCTION_PER_HOUR;
import static com.villagevandals.vandals.resource.Resource.BRICKS;
import static com.villagevandals.vandals.resource.Resource.FOOD;
import static com.villagevandals.vandals.resource.Resource.IRON;
import static com.villagevandals.vandals.resource.Resource.WOOD;

import com.villagevandals.vandals.building.buildings.EconomicProduction;
import com.villagevandals.vandals.village.VillageRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourcesService {
  private static final Logger LOG = LoggerFactory.getLogger(ResourcesService.class);

  VillageRepository repository;

  public ResourcesService(VillageRepository repository) {
    this.repository = repository;
  }

  /**
   * Increases the village's production rate by the building's full production value.
   * Called when a new economic building is first constructed.
   */
  public void updateProduction(EconomicProduction building, long villageId) {
    updateProductionDelta(building, villageId, building.productionPerHour());
  }

  /**
   * Applies an arbitrary {@code delta} to the village's production rate for the resource produced
   * by {@code building}. Used during upgrades where only the incremental gain should be added
   * rather than the building's full production value.
   */
  public void updateProductionDelta(EconomicProduction building, long villageId, int delta) {
    switch (building.producedResource()) {
      case WOOD -> repository.increaseWoodProduction(villageId, delta);
      case FOOD -> repository.increaseFoodProduction(villageId, delta);
      case BRICKS -> repository.increaseBricksProduction(villageId, delta);
      case IRON -> repository.increaseIronProduction(villageId, delta);
      default ->
          throw new IllegalStateException("Unexpected resource: " + building.producedResource());
    }
  }

  /**
   * Calculates the current resource amounts based on elapsed time since the last snapshot
   * without writing anything to the database. Use for display purposes.
   */
  public ResourceStorage getCurrentResourceStorage(long villageId) {
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));
    return calculateStorage(village.getStorage(), village.getProduction(), Instant.now());
  }

  /**
   * Calculates resources accumulated since the last update, writes them to the database,
   * and advances {@code lastUpdate} to now. Call this before any cost deduction to ensure
   * the stored balance reflects the true current amount.
   */
  @Transactional
  public ResourceStorage refreshAndPersist(long villageId) {
    LOG.debug("Refreshing and persisting storage for village {}", villageId);
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));

    Instant now = Instant.now();
    ResourceStorage storage = village.getStorage();
    ResourceStorage updated = calculateStorage(storage, village.getProduction(), now);

    storage.set(WOOD, updated.get(WOOD));
    storage.set(BRICKS, updated.get(BRICKS));
    storage.set(IRON, updated.get(IRON));
    storage.set(FOOD, updated.get(FOOD));
    storage.setLastUpdate(now);

    repository.save(village);
    return storage;
  }

  /**
   * Snapshots the current resource state to the database. Delegates to {@link #refreshAndPersist}.
   * Exists as a named alias to make call sites in the construction flow self-documenting.
   */
  @Transactional
  public void snapshotCurrentResources(long villageId) {
    refreshAndPersist(villageId);
  }

  /**
   * Deducts the given resource costs from the village's stored balance.
   * Validates that all resources are sufficient before applying any deduction —
   * either all costs are applied or none are.
   *
   * @throws IllegalArgumentException if any resource balance is insufficient
   */
  @Transactional
  public void deductResources(long villageId, Map<Resource, Integer> cost) {
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));
    var storage = village.getStorage();

    for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
      int current = storage.get(entry.getKey());
      if (current < entry.getValue()) {
        throw new IllegalArgumentException(
            "Insufficient " + entry.getKey().name().toLowerCase()
            + ": need " + entry.getValue() + ", have " + current);
      }
    }

    for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
      storage.set(entry.getKey(), storage.get(entry.getKey()) - entry.getValue());
    }

    repository.save(village);
  }

  private ResourceStorage calculateStorage(ResourceStorage storage, com.villagevandals.vandals.resource.ResourceProduction production, Instant now) {
    long secondsSinceLastUpdate = Duration.between(storage.getLastUpdate(), now).getSeconds();

    ResourceStorage result = new ResourceStorage();
    result.set(WOOD, updateAmount(storage.get(WOOD), amountProducedSinceLastUpdate(production.getWoodPerHour(), secondsSinceLastUpdate)));
    result.set(BRICKS, updateAmount(storage.get(BRICKS), amountProducedSinceLastUpdate(production.getBricksPerHour(), secondsSinceLastUpdate)));
    result.set(IRON, updateAmount(storage.get(IRON), amountProducedSinceLastUpdate(production.getIronPerHour(), secondsSinceLastUpdate)));
    result.set(FOOD, updateAmount(storage.get(FOOD), amountProducedSinceLastUpdate(production.getFoodPerHour(), secondsSinceLastUpdate)));
    return result;
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
