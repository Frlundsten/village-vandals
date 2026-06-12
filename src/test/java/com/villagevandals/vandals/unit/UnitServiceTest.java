package com.villagevandals.vandals.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.villagevandals.vandals.building.BuildingRepository;
import com.villagevandals.vandals.building.buildings.Barrack;
import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.resource.Resource;
import com.villagevandals.vandals.resource.ResourcesService;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

  @Mock VillageRepository villageRepository;
  @Mock BuildingRepository buildingRepository;
  @Mock UnitRepository unitRepository;
  @Mock TrainingOrderRepository trainingOrderRepository;
  @Mock ResourcesService resourcesService;

  @InjectMocks UnitService unitService;

  private User testUser() {
    return new User(UUID.randomUUID(), "testuser", "test@test.com", "ROLE_USER", new ArrayList<>());
  }

  private Village testVillage() {
    return new Village(0, 0, testUser());
  }

  private TrainingOrder orderWithId(long id, String unitType, long buildingId, Instant finishesAt) {
    return orderWithId(id, unitType, buildingId, finishesAt, 1);
  }

  private TrainingOrder orderWithId(
      long id, String unitType, long buildingId, Instant finishesAt, int quantity) {
    TrainingOrder order = new TrainingOrder();
    ReflectionTestUtils.setField(order, "id", id);
    order.setUnitType(unitType);
    order.setBuildingId(buildingId);
    order.setFinishesAt(finishesAt);
    order.setQuantity(quantity);
    return order;
  }

  // --- trainVandal ---

  @Test
  void trainVandal_createsTrainingOrder_notVandalDirectly() {
    Village village = testVillage();
    TrainingOrder saved = orderWithId(1L, "VANDAL", 10L, Instant.now().plusSeconds(5));

    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    when(trainingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(1L))
        .thenReturn(List.of())
        .thenReturn(List.of(saved));

    List<TrainingOrderDTO> result = unitService.trainVandal(1L, 10L, 1);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().unitType()).isEqualTo("VANDAL");
    assertThat(result.getFirst().quantity()).isEqualTo(1);
    verify(unitRepository, never()).save(any());
    verify(resourcesService).deductResources(eq(1L), eq(Map.of(Resource.FOOD, 50, Resource.IRON, 30)));
  }

  @Test
  void trainVandal_batchQuantity_scalesCostAndDuration() {
    Village village = testVillage();
    Instant now = Instant.now();
    TrainingOrder saved = orderWithId(1L, "VANDAL", 10L, now.plusSeconds(25), 5);

    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    when(trainingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(1L))
        .thenReturn(List.of())
        .thenReturn(List.of(saved));

    List<TrainingOrderDTO> result = unitService.trainVandal(1L, 10L, 5);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().quantity()).isEqualTo(5);
    assertThat(result.getFirst().finishesAt()).isAfterOrEqualTo(now.plusSeconds(25));
    verify(resourcesService).deductResources(eq(1L), eq(Map.of(Resource.FOOD, 250, Resource.IRON, 150)));
  }

  @Test
  void trainVandal_batchExceedsAffordability_rejectsWithoutCreatingOrder() {
    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    doThrow(new IllegalArgumentException("Insufficient food: need 250, have 100"))
        .when(resourcesService).deductResources(eq(1L), eq(Map.of(Resource.FOOD, 250, Resource.IRON, 150)));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> unitService.trainVandal(1L, 10L, 5));

    assertThat(ex.getMessage()).containsIgnoringCase("Insufficient");
    verify(trainingOrderRepository, never()).save(any());
  }

  @Test
  void trainVandal_secondUnit_finishesAtIsChained() {
    Village village = testVillage();
    Instant firstFinishesAt = Instant.now().plusSeconds(3);
    TrainingOrder existing = orderWithId(1L, "VANDAL", 10L, firstFinishesAt);
    TrainingOrder newOrder = orderWithId(2L, "VANDAL", 10L, firstFinishesAt.plusSeconds(5));

    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    when(trainingOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(1L))
        .thenReturn(List.of(existing))
        .thenReturn(List.of(existing, newOrder));

    List<TrainingOrderDTO> result = unitService.trainVandal(1L, 10L, 1);

    assertThat(result).hasSize(2);
    assertThat(result.get(1).finishesAt()).isAfterOrEqualTo(firstFinishesAt.plusSeconds(5));
  }

  @Test
  void trainVandal_wrongBuildingType_throwsIllegalArgumentException() {
    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new LumberMill()));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> unitService.trainVandal(1L, 10L, 1));

    assertThat(ex.getMessage()).containsIgnoringCase("Barrack");
    verify(resourcesService, never()).snapshotCurrentResources(anyLong());
  }

  @Test
  void trainVandal_insufficientResources_propagatesException() {
    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    doThrow(new IllegalArgumentException("Insufficient food: need 50, have 10"))
        .when(resourcesService).deductResources(anyLong(), any());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> unitService.trainVandal(1L, 10L, 1));

    assertThat(ex.getMessage()).containsIgnoringCase("Insufficient");
  }

  // --- resolveCompletedOrders ---

  @Test
  void resolveCompletedOrders_promotesExpiredOrdersToVandals() {
    Village village = spy(testVillage());
    TrainingOrder expired = new TrainingOrder();
    expired.setUnitType("VANDAL");
    expired.setFinishesAt(Instant.now().minusSeconds(1));

    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of(expired));
    when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(villageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    unitService.resolveCompletedOrders(1L);

    assertThat(expired.isCompleted()).isTrue();
    verify(unitRepository).save(any(Vandal.class));
    verify(village).getUnits();
  }

  @Test
  void resolveCompletedOrders_batchOrder_promotesAllUnitsAtOnce() {
    Village village = spy(testVillage());
    TrainingOrder expiredBatch = new TrainingOrder();
    expiredBatch.setUnitType("VANDAL");
    expiredBatch.setFinishesAt(Instant.now().minusSeconds(1));
    expiredBatch.setQuantity(4);

    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of(expiredBatch));
    when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(villageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    unitService.resolveCompletedOrders(1L);

    assertThat(expiredBatch.isCompleted()).isTrue();
    verify(unitRepository, times(4)).save(any(Vandal.class));
    assertThat(village.getUnits()).hasSize(4);
  }

  @Test
  void resolveCompletedOrders_noExpiredOrders_doesNotSaveVillage() {
    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of());

    unitService.resolveCompletedOrders(1L);

    verify(villageRepository, never()).save(any());
    verify(unitRepository, never()).save(any());
  }

  // --- getRoster ---

  @Test
  void getRoster_twoVandals_returnsGroupedRosterEntry() {
    Village village = spy(testVillage());
    doReturn(List.of(new Vandal(), new Vandal())).when(village).getUnits();
    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of());

    List<UnitRosterDTO> roster = unitService.getRoster(1L);

    assertThat(roster).hasSize(1);
    assertThat(roster.getFirst().unitType()).isEqualTo("VANDAL");
    assertThat(roster.getFirst().count()).isEqualTo(2L);
  }

  // --- getTrainingQueue ---

  @Test
  void getTrainingQueue_returnsPendingOrdersSortedByFinishesAt() {
    TrainingOrder o1 = orderWithId(1L, "VANDAL", 10L, Instant.now().plusSeconds(3));
    TrainingOrder o2 = orderWithId(2L, "VANDAL", 10L, Instant.now().plusSeconds(8));

    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of());
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(1L))
        .thenReturn(List.of(o1, o2));

    List<TrainingOrderDTO> queue = unitService.getTrainingQueue(1L);

    assertThat(queue).hasSize(2);
    assertThat(queue.get(0).queuePosition()).isEqualTo(1);
    assertThat(queue.get(1).queuePosition()).isEqualTo(2);
    assertThat(queue.get(0).finishesAt()).isBefore(queue.get(1).finishesAt());
  }

  @Test
  void getTrainingQueue_emptyQueue_returnsEmptyList() {
    when(villageRepository.findById(1L)).thenReturn(Optional.of(testVillage()));
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseAndFinishesAtBefore(eq(1L), any()))
        .thenReturn(List.of());
    when(trainingOrderRepository.findByVillage_IdAndCompletedFalseOrderByFinishesAtAsc(1L))
        .thenReturn(List.of());

    assertThat(unitService.getTrainingQueue(1L)).isEmpty();
  }
}
