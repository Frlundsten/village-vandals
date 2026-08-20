package com.villagevandals.vandals.building;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.building.buildings.Farm;
import com.villagevandals.vandals.building.buildings.LumberMill;
import com.villagevandals.vandals.building.dto.ConstructionRequestDTO;
import com.villagevandals.vandals.building.dto.UpgradeRequestDTO;
import com.villagevandals.vandals.web.GlobalExceptionHandler;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BuildingControllerTest {

  private static final Principal USER = () -> "user";

  MockMvc mvc;

  @Mock BuildingService buildingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc =
        MockMvcBuilders.standaloneSetup(new BuildingController(buildingService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void createBuilding_validRequest_returns200() throws Exception {
    doNothing().when(buildingService).constructBuilding(any(ConstructionRequestDTO.class), eq("user"));

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message.message").value("Constructed building LUMBERMILL successfully"));
  }

  @Test
  void createBuilding_passesAuthenticatedUsernameToTheService() throws Exception {
    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isOk());

    verify(buildingService).constructBuilding(any(ConstructionRequestDTO.class), eq("user"));
  }

  @Test
  void createBuilding_notTheOwner_returns403AndBuildsNothing() throws Exception {
    doThrow(new AccessDeniedException("Not the owner of village 1"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class), anyString());

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void createBuilding_siteAlreadyOccupied_returns400() throws Exception {
    doThrow(new IllegalArgumentException("A building already exists on this site"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class), anyString());

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createBuilding_villageNotFound_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Village Not Found"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class), anyString());

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"LUMBERMILL\",\"constructionSiteId\":1,\"villageId\":999}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createBuilding_insufficientResources_returns400WithTheActualReason() throws Exception {
    doThrow(new IllegalArgumentException("Insufficient wood: need 60, have 10"))
        .when(buildingService)
        .constructBuilding(any(ConstructionRequestDTO.class), anyString());

    mvc.perform(
            post("/building")
                .contentType(APPLICATION_JSON)
                .principal(USER)
                .content("{\"type\":\"FARM\",\"constructionSiteId\":1,\"villageId\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message.message").value("Insufficient wood: need 60, have 10"));
  }

  @Test
  void getAvailableBuildings_returnsListOfBuildings() throws Exception {
    when(buildingService.getAvailableBuildings(eq(1L), any()))
        .thenReturn(List.of(new LumberMill(), new Farm()));

    mvc.perform(get("/building/available").param("villageId", "1").principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getAvailableBuildings_farmHasCorrectConstructionCost() throws Exception {
    when(buildingService.getAvailableBuildings(eq(1L), any())).thenReturn(List.of(new Farm()));

    mvc.perform(get("/building/available").param("villageId", "1").principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].constructionCost.wood").value(60))
        .andExpect(jsonPath("$[0].constructionCost.food").value(40))
        .andExpect(jsonPath("$[0].upgradeCost").doesNotExist());
  }

  @Test
  void getAvailableBuildings_lumberMillHasCorrectConstructionCost() throws Exception {
    when(buildingService.getAvailableBuildings(eq(1L), any())).thenReturn(List.of(new LumberMill()));

    mvc.perform(get("/building/available").param("villageId", "1").principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].constructionCost.food").value(50))
        .andExpect(jsonPath("$[0].constructionCost.bricks").value(60))
        .andExpect(jsonPath("$[0].upgradeCost").doesNotExist());
  }

  @Test
  void getExistingBuildings_returnsBuildingsMappedBySiteId() throws Exception {
    when(buildingService.getAllBuildingsByVillageId(eq(1L), eq("user")))
        .thenReturn(Map.of(1L, new LumberMill(), 2L, new Farm()));

    mvc.perform(get("/building").param("villageId", "1").principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getExistingBuildings_notTheOwner_returns403() throws Exception {
    when(buildingService.getAllBuildingsByVillageId(eq(1L), eq("user")))
        .thenThrow(new AccessDeniedException("Not the owner of village 1"));

    mvc.perform(get("/building").param("villageId", "1").principal(USER))
        .andExpect(status().isForbidden());
  }

  @Test
  void upgradeBuilding_success_returns200WithDTO() throws Exception {
    Farm upgraded = new Farm();
    upgraded.upgrade();
    when(buildingService.upgradeBuilding(any(UpgradeRequestDTO.class), any())).thenReturn(upgraded);

    mvc.perform(
            post("/building/upgrade")
                .contentType(APPLICATION_JSON)
                .content("{\"villageId\":1,\"constructionSiteId\":2}")
                .principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("FARM"))
        .andExpect(jsonPath("$.level").value(2))
        .andExpect(jsonPath("$.upgradeCost").exists());
  }

  @Test
  void upgradeBuilding_insufficientResources_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Insufficient wood: need 200, have 50"))
        .when(buildingService)
        .upgradeBuilding(any(UpgradeRequestDTO.class), any());

    mvc.perform(
            post("/building/upgrade")
                .contentType(APPLICATION_JSON)
                .content("{\"villageId\":1,\"constructionSiteId\":2}")
                .principal(USER))
        .andExpect(status().isBadRequest());
  }

  @Test
  void upgradeBuilding_notTheOwner_returns403() throws Exception {
    doThrow(new AccessDeniedException("Not the owner of village 1"))
        .when(buildingService)
        .upgradeBuilding(any(UpgradeRequestDTO.class), any());

    mvc.perform(
            post("/building/upgrade")
                .contentType(APPLICATION_JSON)
                .content("{\"villageId\":1,\"constructionSiteId\":2}")
                .principal(USER))
        .andExpect(status().isForbidden());
  }

  /** "No building at this site" is a conflict with current state, not a malformed request. */
  @Test
  void upgradeBuilding_noBuilding_returns409() throws Exception {
    doThrow(new IllegalStateException("No building to upgrade at this site"))
        .when(buildingService)
        .upgradeBuilding(any(UpgradeRequestDTO.class), any());

    mvc.perform(
            post("/building/upgrade")
                .contentType(APPLICATION_JSON)
                .content("{\"villageId\":1,\"constructionSiteId\":99}")
                .principal(USER))
        .andExpect(status().isConflict());
  }

  @Test
  void deleteBuilding_validRequest_returns200WithMessage() throws Exception {
    doNothing().when(buildingService).deleteBuilding(eq(1L), eq(2L), eq("user"));

    mvc.perform(
            delete("/building")
                .param("villageId", "1")
                .param("constructionSiteId", "2")
                .principal(USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message.message").value("Demolished building successfully"));

    verify(buildingService).deleteBuilding(1L, 2L, "user");
  }

  @Test
  void deleteBuilding_noBuildingAtSite_returns409() throws Exception {
    doThrow(new IllegalStateException("No building to delete at this site"))
        .when(buildingService)
        .deleteBuilding(anyLong(), anyLong(), any());

    mvc.perform(
            delete("/building")
                .param("villageId", "1")
                .param("constructionSiteId", "99")
                .principal(USER))
        .andExpect(status().isConflict());
  }

  @Test
  void deleteBuilding_notTheOwner_returns403() throws Exception {
    doThrow(new AccessDeniedException("Not the owner of village 1"))
        .when(buildingService)
        .deleteBuilding(anyLong(), anyLong(), any());

    mvc.perform(
            delete("/building")
                .param("villageId", "1")
                .param("constructionSiteId", "2")
                .principal(USER))
        .andExpect(status().isForbidden());
  }
}
