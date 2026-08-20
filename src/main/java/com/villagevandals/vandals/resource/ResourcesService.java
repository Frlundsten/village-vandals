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

/**
 * Resources are not produced by a background job. A village stores an amount plus a
 * {@code lastUpdate} timestamp, and production is credited lazily from the elapsed time whenever
 * something reads or spends resources.
 *
 * <p><b>Accounting invariant:</b> the time credited and the time consumed are always the same.
 * Elapsed time is truncated to whole seconds, and {@code lastUpdate} advances by exactly those
 * seconds rather than jumping to "now", so the sub-second remainder carries into the next call.
 * Without that, two actions inside one second would credit nothing while still consuming the time.
 *
 * <p>Truncating to whole seconds is loss-free only while every production rate is a multiple of
 * {@code DEFAULT_PRODUCTION_PER_HOUR} (3600) per hour — one second then always yields a whole
 * number of units. That holds for every rate the game currently produces
 * ({@code DEFAULT_BASE_PRODUCTION_RATE} 3600 and {@code DEFAULT_ECONOMICAL_PRODUCTION_RATE} 18000).
 * A rate that is not a multiple of 3600 would lose sub-unit remainders and would need fractional
 * storage instead.
 */
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
   * by {@code building}. Updates the entity directly so Hibernate's flush at transaction commit
   * does not overwrite the change (which native SQL updates would cause via cache bypass).
   */
  public void updateProductionDelta(EconomicProduction building, long villageId, int delta) {
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));
    ResourceProduction production = village.getProduction();
    switch (building.producedResource()) {
      case WOOD -> production.setWoodPerHour(production.getWoodPerHour() + delta);
      case FOOD -> production.setFoodPerHour(production.getFoodPerHour() + delta);
      case BRICKS -> production.setBricksPerHour(production.getBricksPerHour() + delta);
      case IRON -> production.setIronPerHour(production.getIronPerHour() + delta);
      default ->
          throw new IllegalStateException("Unexpected resource: " + building.producedResource());
    }
    repository.save(village);
  }

  public ResourceProduction getProduction(long villageId) {
    return repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"))
        .getProduction();
  }

  /**
   * Calculates the current resource amounts based on elapsed time since the last snapshot
   * without writing anything to the database. Use for display purposes.
   */
  public ResourceStorage getCurrentResourceStorage(long villageId) {
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));
    return accrueSince(village.getStorage(), village.getProduction(), Instant.now()).storage();
  }

  /**
   * Calculates resources accumulated since the last update, writes them to the database, and
   * advances {@code lastUpdate} by exactly the number of seconds credited. Call this before any
   * cost deduction to ensure the stored balance reflects the true current amount.
   */
  @Transactional
  public ResourceStorage refreshAndPersist(long villageId) {
    LOG.debug("Refreshing and persisting storage for village {}", villageId);
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));

    ResourceStorage storage = village.getStorage();
    Accrual accrual = accrueSince(storage, village.getProduction(), Instant.now());

    storage.set(WOOD, accrual.storage().get(WOOD));
    storage.set(BRICKS, accrual.storage().get(BRICKS));
    storage.set(IRON, accrual.storage().get(IRON));
    storage.set(FOOD, accrual.storage().get(FOOD));
    // Consume exactly the time that was credited; the remainder rolls into the next call.
    storage.setLastUpdate(storage.getLastUpdate().plusSeconds(accrual.creditedSeconds()));

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
   * Validates that all costs are non-negative and all resources are sufficient before applying any
   * deduction — either all costs are applied or none are.
   *
   * <p>The non-negative check matters: a negative cost would pass the affordability test and then
   * *credit* the village, so any caller that computes a cost with overflowing arithmetic would be
   * handing out free resources.
   *
   * @throws IllegalArgumentException if any cost is negative or any resource balance is insufficient
   */
  @Transactional
  public void deductResources(long villageId, Map<Resource, Integer> cost) {
    var village = repository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found"));
    var storage = village.getStorage();

    for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
      Integer amount = entry.getValue();
      if (amount == null || amount < 0) {
        throw new IllegalArgumentException(
            "Cost for " + entry.getKey().name().toLowerCase() + " must not be negative, was "
            + amount);
      }
      int current = storage.get(entry.getKey());
      if (current < amount) {
        throw new IllegalArgumentException(
            "Insufficient " + entry.getKey().name().toLowerCase()
            + ": need " + amount + ", have " + current);
      }
    }

    for (Map.Entry<Resource, Integer> entry : cost.entrySet()) {
      storage.set(entry.getKey(), storage.get(entry.getKey()) - entry.getValue());
    }

    repository.save(village);
  }

  /** Amounts accrued up to a whole-second boundary, and how many seconds that accounted for. */
  private record Accrual(ResourceStorage storage, long creditedSeconds) {}

  private Accrual accrueSince(
      ResourceStorage storage, ResourceProduction production, Instant now) {
    long elapsedSeconds = wholeSecondsSince(storage.getLastUpdate(), now);

    ResourceStorage result = new ResourceStorage();
    result.set(WOOD, accrue(storage.get(WOOD), production.getWoodPerHour(), elapsedSeconds));
    result.set(BRICKS, accrue(storage.get(BRICKS), production.getBricksPerHour(), elapsedSeconds));
    result.set(IRON, accrue(storage.get(IRON), production.getIronPerHour(), elapsedSeconds));
    result.set(FOOD, accrue(storage.get(FOOD), production.getFoodPerHour(), elapsedSeconds));
    return new Accrual(result, elapsedSeconds);
  }

  /**
   * Whole seconds between the two instants, never negative — a {@code lastUpdate} in the future
   * (clock adjustment or a bad write) must not subtract resources.
   */
  private long wholeSecondsSince(Instant lastUpdate, Instant now) {
    return Math.max(0L, Duration.between(lastUpdate, now).getSeconds());
  }

  /**
   * Adds the production accrued over {@code elapsedSeconds} to {@code storedAmount}, saturating at
   * {@link Integer#MAX_VALUE}. All arithmetic is done in {@code long}: a long-idle village at a
   * high rate would otherwise overflow {@code int} and end up with a negative balance.
   */
  private int accrue(int storedAmount, int ratePerHour, long elapsedSeconds) {
    if (ratePerHour <= 0 || elapsedSeconds <= 0) {
      return storedAmount;
    }
    // Cap the multiplicand so rate * seconds cannot overflow long for an absurd lastUpdate.
    long safeSeconds = Math.min(elapsedSeconds, Long.MAX_VALUE / ratePerHour);
    long produced = (ratePerHour * safeSeconds) / DEFAULT_PRODUCTION_PER_HOUR;
    return (int) Math.min((long) storedAmount + produced, Integer.MAX_VALUE);
  }
}
