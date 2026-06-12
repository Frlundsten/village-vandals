package com.villagevandals.vandals.unit;

import static com.villagevandals.vandals.gameconfig.GameDefaults.TRAINING_DURATION_SECONDS;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_FOOD_COST;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_IRON_COST;

import com.villagevandals.vandals.building.BuildingRepository;
import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.resource.Resource;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.village.Village;
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
  private final BuildingRepository buildingRepository;
  private final UnitRepository unitRepository;
  private final TrainingOrderRepository trainingOrderRepository;
  private final ResourcesService resourcesService;

  public UnitService(
      VillageRepository villageRepository,
      BuildingRepository buildingRepository,
      UnitRepository unitRepository,
      TrainingOrderRepository trainingOrderRepository,
      ResourcesService resourcesService) {
    this.villageRepository = villageRepository;
    this.buildingRepository = buildingRepository;
    this.unitRepository = unitRepository;
    this.trainingOrderRepository = trainingOrderRepository;
    this.resourcesService = resourcesService;
  }

  @Transactional
  public List<TrainingOrderDTO> trainVandal(long villageId, long buildingId, int quantity) {
    if (quantity < 1) {
      throw new IllegalArgumentException("Quantity must be at least 1");
    }

    Village village = findVillageOrThrow(villageId);
    requireBarrack(buildingId);

    resourcesService.snapshotCurrentResources(villageId);
    resourcesService.deductResources(
        villageId,
        Map.of(
            Resource.FOOD, VANDAL_FOOD_COST * quantity,
            Resource.IRON, VANDAL_IRON_COST * quantity));

    List<TrainingOrder> existingQueue =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);

    Instant finishesAt = computeNextFinishesAt(existingQueue, quantity);

    TrainingOrder order = new TrainingOrder();
    order.setVillage(village);
    order.setBuildingId(buildingId);
    order.setUnitType("VANDAL");
    order.setQueuedAt(Instant.now());
    order.setFinishesAt(finishesAt);
    order.setQuantity(quantity);
    trainingOrderRepository.save(order);

    List<TrainingOrder> updatedQueue =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);
    return toOrderDTOs(updatedQueue);
  }

  @Transactional
  public List<TrainingOrderDTO> getTrainingQueue(long villageId) {
    resolveCompletedOrders(villageId);
    List<TrainingOrder> pending =
        trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(villageId);
    return toOrderDTOs(pending);
  }

  @Transactional
  public List<UnitRosterDTO> getRoster(long villageId) {
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

  private Instant computeNextFinishesAt(List<TrainingOrder> existingQueue, int quantity) {
    long durationSeconds = (long) TRAINING_DURATION_SECONDS * quantity;
    if (existingQueue.isEmpty()) {
      return Instant.now().plusSeconds(durationSeconds);
    }
    return existingQueue.getLast().getFinishesAt().plusSeconds(durationSeconds);
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

  private void requireBarrack(long buildingId) {
    var building = buildingRepository.findById(buildingId)
        .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingId));
    if (!(building instanceof Barrack)) {
      throw new IllegalArgumentException(
          "Building " + buildingId + " is not a Barrack — cannot train units here");
    }
  }
}
