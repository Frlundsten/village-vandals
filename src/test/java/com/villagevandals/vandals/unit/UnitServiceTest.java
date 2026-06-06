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

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

  @Mock VillageRepository villageRepository;
  @Mock BuildingRepository buildingRepository;
  @Mock UnitRepository unitRepository;
  @Mock ResourcesService resourcesService;

  @InjectMocks UnitService unitService;

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private User testUser() {
    return new User(UUID.randomUUID(), "testuser", "test@test.com", "ROLE_USER", new ArrayList<>());
  }

  private Village testVillage() {
    return new Village(0, 0, testUser());
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void trainVandal_success_returnsTrainResponseDTO() {
    Village village = testVillage();
    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    when(unitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(villageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TrainResponseDTO result = unitService.trainVandal(1L, 10L);

    assertThat(result).isNotNull();
    assertThat(result.unitType()).isEqualTo("VANDAL");
    assertThat(result.hp()).isEqualTo(4);
    assertThat(result.damage()).isEqualTo(1);
    assertThat(result.villageId()).isEqualTo(1L);
    verify(resourcesService).snapshotCurrentResources(1L);
    verify(resourcesService).deductResources(eq(1L), eq(Map.of(Resource.FOOD, 50, Resource.IRON, 30)));
  }

  @Test
  void trainVandal_wrongBuildingType_throwsIllegalArgumentException() {
    Village village = testVillage();
    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new LumberMill()));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> unitService.trainVandal(1L, 10L));

    assertThat(ex.getMessage()).containsIgnoringCase("Barrack");
    verify(resourcesService, never()).snapshotCurrentResources(anyLong());
  }

  @Test
  void trainVandal_insufficientResources_propagatesException() {
    Village village = testVillage();
    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));
    when(buildingRepository.findById(10L)).thenReturn(Optional.of(new Barrack()));
    doThrow(new IllegalArgumentException("Insufficient food: need 50, have 10"))
        .when(resourcesService).deductResources(anyLong(), any());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> unitService.trainVandal(1L, 10L));

    assertThat(ex.getMessage()).containsIgnoringCase("Insufficient");
  }

  @Test
  void getRoster_twoVandals_returnsGroupedRosterEntry() {
    Vandal vandal1 = new Vandal();
    Vandal vandal2 = new Vandal();
    Village village = spy(testVillage());
    doReturn(List.of(vandal1, vandal2)).when(village).getUnits();
    when(villageRepository.findById(1L)).thenReturn(Optional.of(village));

    List<UnitRosterDTO> roster = unitService.getRoster(1L);

    assertThat(roster).hasSize(1);
    UnitRosterDTO entry = roster.getFirst();
    assertThat(entry.unitType()).isEqualTo("VANDAL");
    assertThat(entry.count()).isEqualTo(2L);
    assertThat(entry.hp()).isEqualTo(4);
    assertThat(entry.damage()).isEqualTo(1);
  }
}
