package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_ECONOMICAL_PRODUCTION_RATE;
import static com.villagevandals.vandals.resource.Resource.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.villagevandals.vandals.building.buildings.LumberMill;
import java.util.Map;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ResourcesServiceTest {

  @Mock VillageRepository repository;
  @Mock User mockUser;

  ResourcesService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new ResourcesService(repository);
  }

  @Test
  void getCurrentResourceStorage_noProduction_returnsOnlyStartingAmount() {
    Village village = villageWithProductionRate(0, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    ResourceStorage result = service.getCurrentResourceStorage(1L);

    assertThat(result.get(WOOD)).isEqualTo(100);
    assertThat(result.get(BRICKS)).isEqualTo(100);
    assertThat(result.get(IRON)).isEqualTo(100);
    assertThat(result.get(FOOD)).isEqualTo(100);
  }

  @Test
  void getCurrentResourceStorage_withWoodProduction_addsProducedWood() {
    // LumberMill produces 450 wood/hour. After 2 hours → 900 produced.
    Village village = villageWithWoodRate(450, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    ResourceStorage result = service.getCurrentResourceStorage(1L);

    assertThat(result.get(WOOD)).isBetween(1000, 1002); // 100 + 900, small timing tolerance
    assertThat(result.get(BRICKS)).isEqualTo(100); // unchanged
  }

  @Test
  void refreshAndPersist_persistsUpdatedStorageAndLastUpdate() {
    Village village = villageWithWoodRate(450, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    ResourceStorage result = service.refreshAndPersist(1L);

    verify(repository).save(village);
    assertThat(result.get(WOOD)).isGreaterThan(100);
    assertThat(village.getStorage().getLastUpdate()).isAfter(twoHoursAgo());
  }

  @Test
  void refreshAndPersist_calledTwiceImmediately_doesNotDoubleCount() {
    Village village = villageWithWoodRate(450, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    ResourceStorage first = service.refreshAndPersist(1L);
    int woodAfterFirst = first.get(WOOD);

    // Simulate second call immediately after — lastUpdate is now, so 0 seconds elapsed
    ResourceStorage second = service.refreshAndPersist(1L);

    assertThat(second.get(WOOD)).isEqualTo(woodAfterFirst);
  }

  @Test
  void updateProduction_wood_increasesWoodProductionInDb() {
    LumberMill mill = new LumberMill();
    service.updateProduction(mill, 1L);
    verify(repository).increaseWoodProduction(1L, mill.productionPerHour());
  }

  @Test
  void snapshotCurrentResources_savesVillage() {
    Village village = villageWithProductionRate(0, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    service.snapshotCurrentResources(1L);

    verify(repository).save(any(Village.class));
  }

  @Test
  void deductResources_sufficientFunds_deductsCorrectly() {
    Village village = villageWithProductionRate(0, Instant.now());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    service.deductResources(1L, Map.of(WOOD, 60, FOOD, 40));

    assertThat(village.getStorage().get(WOOD)).isEqualTo(40);  // 100 - 60
    assertThat(village.getStorage().get(FOOD)).isEqualTo(60);  // 100 - 40
    assertThat(village.getStorage().get(BRICKS)).isEqualTo(100); // untouched
    verify(repository).save(village);
  }

  @Test
  void deductResources_insufficientFunds_throwsWithMessage() {
    Village village = villageWithProductionRate(0, Instant.now());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    assertThatThrownBy(() -> service.deductResources(1L, Map.of(WOOD, 200)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insufficient wood")
        .hasMessageContaining("need 200")
        .hasMessageContaining("have 100");
  }

  @Test
  void deductResources_exactAmount_leavesZero() {
    Village village = villageWithProductionRate(0, Instant.now());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    service.deductResources(1L, Map.of(WOOD, 100));

    assertThat(village.getStorage().get(WOOD)).isEqualTo(0);
  }

  @Test
  void getCurrentResourceStorage_starterVillageDefaultRates_producesAllResourcesOverTime() {
    Village village = villageWithAllRates(DEFAULT_ECONOMICAL_PRODUCTION_RATE, twoHoursAgo());
    when(repository.findById(1L)).thenReturn(Optional.of(village));

    ResourceStorage result = service.getCurrentResourceStorage(1L);

    assertThat(result.get(WOOD)).isGreaterThan(100);
    assertThat(result.get(FOOD)).isGreaterThan(100);
    assertThat(result.get(BRICKS)).isGreaterThan(100);
    assertThat(result.get(IRON)).isGreaterThan(100);
  }

  private Village villageWithAllRates(int rate, Instant lastUpdate) {
    ResourceStorage storage = new ResourceStorage();
    storage.setLastUpdate(lastUpdate);
    ResourceProduction production = new ResourceProduction();
    production.setWoodPerHour(rate);
    production.setFoodPerHour(rate);
    production.setBricksPerHour(rate);
    production.setIronPerHour(rate);
    return new Village(0, 0, mockUser, storage, production);
  }

  private Village villageWithProductionRate(int rate, Instant lastUpdate) {
    ResourceStorage storage = new ResourceStorage();
    storage.setLastUpdate(lastUpdate);
    ResourceProduction production = new ResourceProduction();
    return new Village(0, 0, mockUser, storage, production);
  }

  private Village villageWithWoodRate(int woodRate, Instant lastUpdate) {
    ResourceStorage storage = new ResourceStorage();
    storage.setLastUpdate(lastUpdate);
    ResourceProduction production = new ResourceProduction();
    production.setWoodPerHour(woodRate);
    return new Village(0, 0, mockUser, storage, production);
  }

  private Instant twoHoursAgo() {
    return Instant.now().minus(2, ChronoUnit.HOURS);
  }
}
