package com.villagevandals.vandals.building;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BuildingControllerTest {

  MockMvc mvc;

  @Mock BuildingService buildingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(new BuildingController(buildingService)).build();
  }

  @Test
  void createBuilding_validRequest_returns200() throws Exception {
    doNothing().when(buildingService).constructBuilding(any(ConstructionRequestDTO.class));

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message.message").value("Constructed building LUMBERMILL successfully"));
  }

  @Test
  void createBuilding_siteAlreadyOccupied_returns400() throws Exception {
    doThrow(new IllegalArgumentException("A building already exists on this site"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class));

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createBuilding_villageNotFound_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Village Not Found"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class));

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":999}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createBuilding_insufficientResources_returns400WithMessage() throws Exception {
    doThrow(new IllegalArgumentException("Insufficient wood: need 60, have 10"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class));

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .content("{\"type\":\"FARM\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$").value("Unable to construct building"));
  }

  @Test
  void getAvailableBuildings_returnsListOfBuildings() throws Exception {
    when(buildingService.getAvailableBuildings(eq(1L), any()))
        .thenReturn(List.of(new LumberMill(), new Farm()));

    mvc.perform(get("/building/available").param("villageId", "1").principal(() -> "user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getExistingBuildings_returnsBuildingsMappedBySiteId() throws Exception {
    when(buildingService.getAllBuildingsByVillageId(eq(1L), eq("user")))
        .thenReturn(Map.of(1L, new LumberMill(), 2L, new Farm()));

    mvc.perform(get("/building").param("villageId", "1").principal(() -> "user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
