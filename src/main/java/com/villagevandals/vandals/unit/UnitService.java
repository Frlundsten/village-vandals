package com.villagevandals.vandals.unit;

import static com.villagevandals.vandals.gameconfig.GameDefaults.MAX_TRAINING_BATCH_SIZE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.TRAINING_DURATION_SECONDS;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_FOOD_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_IRON_COST;

import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.resource.Resource;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageOwnershipService;
import com.villagevandals.vandals.village.VillageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnitService {

  private final VillageRepository villageRepository;
  private final ConstructionSiteRepository constructionSiteRepository;
  private final UnitRepository unitRepository;
  private final TrainingOrderRepository trainingOrderRepository;
  private final ResourcesService resourcesService;
  private final VillageOwnershipService villageOwnershipService;

  public UnitService(
      VillageRepository villageRepository,
      ConstructionSiteRepository constructionSiteRepository,
      UnitRepository unitRepository,
      TrainingOrderRepository trainingOrderRepository,
      ResourcesService resourcesService,
      VillageOwnershipService villageOwnershipService) {
    this.villageRepository = villageRepository;
    this.constructionSiteRepository = constructionSiteRepository;
    this.unitRepository = unitRepository;
    this.trainingOrderRepository = trainingOrderRepository;
    this.resourcesService = resourcesService;
    this.villageOwnershipService = villageOwnershipService;
  }

  /**
   * Queues a training order for {@code quantity} Vandals at a Barrack in the village.
   *
   * <p>Validation happens before any state changes, in this order: the caller must own the village,
   * the batch size must be within bounds, and the building must be a Barrack standing in *this*
   * village. Only then are resources snapshotted and deducted.
   *
   * @throws org.springframework.security.access.AccessDeniedException if {@code username} does not
   *     own the village
   * @throws IllegalArgumentException if the batch size is out of range, the building is not a
   *     Barrack in this village, or resources are insufficient
   */
  @Transactional
  public List<TrainingOrderDTO> trainVandal(
      long villageId, long buildingId, int quantity, String username) {
    villageOwnershipService.requireOwner(villageId, username);
    requireValidQuantity(quantity);

    Village village = findVillageOrThrow(villageId);
    requireBarrackInVillage(villageId, buildingId);

    // Promote anything that has already finished first, so a stale pending order cannot anchor
    // the queue chain in the past and make the new order complete instantly.
    resolveCompletedOrders(villageId);

    resourcesService.snapshotCurrentResources(villageId);
    resourcesService.deductResources(villageId, vandalCost(quantity));

    List<TrainingOrder> existingQueue =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);

    TrainingOrder order = new TrainingOrder();
    order.setVillage(village);
    order.setBuildingId(buildingId);
    order.setUnitType("VANDAL");
    order.setQueuedAt(Instant.now());
    order.setFinishesAt(computeNextFinishesAt(existingQueue, quantity));
    order.setQuantity(quantity);
    trainingOrderRepository.save(order);

    List<TrainingOrder> updatedQueue =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);
    return toOrderDTOs(updatedQueue);
  }

  @Transactional
  public List<TrainingOrderDTO> getTrainingQueue(long villageId, String username) {
    villageOwnershipService.requireOwner(villageId, username);
    resolveCompletedOrders(villageId);
    List<TrainingOrder> pending =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);
    return toOrderDTOs(pending);
  }

  @Transactional
  public List<UnitRosterDTO> getRoster(long villageId, String username) {
    villageOwnershipService.requireOwner(villageId, username);
    resolveCompletedOrders(villageId);
    Village village = findVillageOrThrow(villageId);
    return village.getUnits().stream()
        .collect(Collectors.groupingBy(VillageUnit::getUnitType, Collectors.counting()))
        .entrySet().stream()
        .map(entry -> {
          String unitType = entry.getKey();
          long count = entry.getValue();
          VillageUnit representative = village.getUnits().stream()
              .filter(u -> u.getUnitType().equals(unitType))
              .findFirst()
              .orElseThrow();
          return new UnitRosterDTO(unitType, count, representative.getHp(), representative.getDamage());
        })
        .toList();
  }

  /**
   * Promotes every pending order whose {@code finishesAt} has passed into actual units. Training
   * has no background job — this runs lazily whenever the queue or roster is read.
   */
  @Transactional
  public void resolveCompletedOrders(long villageId) {
    Village village = findVillageOrThrow(villageId);
    List<TrainingOrder> expired =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(
            villageId, Instant.now());

    for (TrainingOrder order : expired) {
      for (int i = 0; i < order.getQuantity(); i++) {
        VillageUnit unit = createUnitForType(order.getUnitType());
        unitRepository.save(unit);
        village.getUnits().add(unit);
      }
      order.setCompleted(true);
    }

    if (!expired.isEmpty()) {
      villageRepository.save(village);
    }
  }

  /**
   * Rejects batch sizes outside {@code 1..MAX_TRAINING_BATCH_SIZE}.
   *
   * <p>The upper bound is a safety bound, not only a balance one: {@code VANDAL_FOOD_COST *
   * quantity} is {@code int} arithmetic, so a large enough quantity wraps the cost negative, slips
   * past the affordability check, and *credits* resources instead of deducting them.
   */
  private void requireValidQuantity(int quantity) {
    if (quantity < 1 || quantity > MAX_TRAINING_BATCH_SIZE) {
      throw new IllegalArgumentException(
          "Quantity must be between 1 and " + MAX_TRAINING_BATCH_SIZE + ", was " + quantity);
    }
  }

  private Map<Resource, Integer> vandalCost(int quantity) {
    return Map.of(
        Resource.FOOD, VANDAL_FOOD_COST * quantity,
        Resource.IRON, VANDAL_IRON_COST * quantity);
  }

  /**
   * Returns the instant the new order finishes: one duration after the last pending order, but
   * never earlier than one duration from now.
   *
   * <p>The clamp matters because orders are only resolved on read. A player who trains, closes the
   * client, and comes back has a pending order whose {@code finishesAt} is already in the past;
   * chaining off it unclamped would produce an order that is complete the moment it is created.
   */
  private Instant computeNextFinishesAt(List<TrainingOrder> existingQueue, int quantity) {
    long durationSeconds = (long) TRAINING_DURATION_SECONDS * quantity;
    Instant now = Instant.now();
    Instant chainBase =
        existingQueue.isEmpty()
            ? now
            : maxOf(now, existingQueue.getLast().getFinishesAt());
    return chainBase.plusSeconds(durationSeconds);
  }

  private Instant maxOf(Instant a, Instant b) {
    return a.isAfter(b) ? a : b;
  }

  private VillageUnit createUnitForType(String unitType) {
    return switch (unitType) {
      case "VANDAL" -> new Vandal();
      default -> throw new IllegalArgumentException("Unknown unit type: " + unitType);
    };
  }

  private List<TrainingOrderDTO> toOrderDTOs(List<TrainingOrder> orders) {
    Instant serverTime = Instant.now();
    return IntStream.range(0, orders.size())
        .mapToObj(index -> {
          TrainingOrder order = orders.get(index);
          return new TrainingOrderDTO(
              order.getId(),
              order.getUnitType(),
              order.getBuildingId(),
              order.getFinishesAt(),
              order.getQuantity(),
              index + 1,
              serverTime);
        })
        .toList();
  }

  private Village findVillageOrThrow(long villageId) {
    return villageRepository.findById(villageId)
        .orElseThrow(() -> new IllegalArgumentException("Village not found: " + villageId));
  }

  /**
   * Resolves the building through the village's own construction sites, so a Barrack standing in
   * someone else's village cannot be used as a training location.
   */
  private void requireBarrackInVillage(long villageId, long buildingId) {
    ConstructionSite site =
        constructionSiteRepository
            .findByVillage_IdAndBuilding_Id(villageId, buildingId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Building " + buildingId + " is not in village " + villageId));

    if (!(site.getBuilding() instanceof Barrack)) {
      throw new IllegalArgumentException(
          "Building " + buildingId + " is not a Barrack — cannot train units here");
    }
  }
}
