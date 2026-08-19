package com.villagevandals.vandals.unit;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UnitControllerTest {

  MockMvc mvc;

  @Mock UnitService unitService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(new UnitController(unitService)).build();
  }

  @Test
  void getTrainingQueue_returnsOrdersSortedByFinishesAt() throws Exception {
    Instant t1 = Instant.parse("2099-01-01T00:00:05Z");
    Instant t2 = Instant.parse("2099-01-01T00:00:10Z");
    Instant serverTime = Instant.now();
    when(unitService.getTrainingQueue(42L)).thenReturn(List.of(
        new TrainingOrderDTO(1L, "VANDAL", 10L, t1, 1, 1, serverTime),
        new TrainingOrderDTO(2L, "VANDAL", 10L, t2, 1, 2, serverTime)));

    mvc.perform(get("/unit/training").param("villageId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].queuePosition").value(1))
        .andExpect(jsonPath("$[1].queuePosition").value(2))
        .andExpect(jsonPath("$[0].serverTime").exists());
  }

  @Test
  void getTrainingQueue_emptyQueue_returnsEmptyList() throws Exception {
    when(unitService.getTrainingQueue(42L)).thenReturn(List.of());

    mvc.perform(get("/unit/training").param("villageId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void trainUnit_validRequest_returnsQueueWithNewOrder() throws Exception {
    Instant finish = Instant.parse("2099-01-01T00:00:05Z");
    when(unitService.trainVandal(1L, 10L, 1)).thenReturn(
        List.of(new TrainingOrderDTO(1L, "VANDAL", 10L, finish, 1, 1, Instant.now())));

    mvc.perform(post("/unit/train").contentType(APPLICATION_JSON)
            .content("{\"villageId\":1,\"buildingId\":10,\"quantity\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].unitType").value("VANDAL"))
        .andExpect(jsonPath("$[0].quantity").value(1))
        .andExpect(jsonPath("$[0].queuePosition").value(1))
        .andExpect(jsonPath("$[0].serverTime").exists());
  }

  @Test
  void trainUnit_batchRequest_passesQuantityThroughAndReturnsBatchOrder() throws Exception {
    Instant finish = Instant.parse("2099-01-01T00:01:05Z");
    when(unitService.trainVandal(1L, 10L, 5)).thenReturn(
        List.of(new TrainingOrderDTO(1L, "VANDAL", 10L, finish, 5, 1, Instant.now())));

    mvc.perform(post("/unit/train").contentType(APPLICATION_JSON)
            .content("{\"villageId\":1,\"buildingId\":10,\"quantity\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].quantity").value(5))
        .andExpect(jsonPath("$[0].serverTime").exists());
  }

  @Test
  void trainUnit_invalidBuilding_returnsBadRequest() throws Exception {
    when(unitService.trainVandal(1L, 99L, 1))
        .thenThrow(new IllegalArgumentException("Building 99 is not a Barrack"));

    mvc.perform(post("/unit/train").contentType(APPLICATION_JSON)
            .content("{\"villageId\":1,\"buildingId\":99,\"quantity\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getRoster_returnsGroupedEntries() throws Exception {
    when(unitService.getRoster(42L)).thenReturn(
        List.of(new UnitRosterDTO("VANDAL", 3L, 4, 1)));

    mvc.perform(get("/unit").param("villageId", "42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].unitType").value("VANDAL"))
        .andExpect(jsonPath("$[0].count").value(3));
  }
}
