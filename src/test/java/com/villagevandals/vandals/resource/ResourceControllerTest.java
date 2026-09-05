package com.villagevandals.vandals.resource;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.village.VillageOwnershipService;
import com.villagevandals.vandals.web.GlobalExceptionHandler;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResourceControllerTest {

  private static final Principal ALICE = () -> "alice";

  MockMvc mvc;

  @Mock ResourcesService resourcesService;
  @Mock VillageOwnershipService villageOwnershipService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new ResourceController(resourcesService, villageOwnershipService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void refresh_returnsAmountsAndProductionRates() throws Exception {
    var storage = new ResourceStorage();
    storage.set(Resource.FOOD, 100);
    storage.set(Resource.WOOD, 200);
    storage.set(Resource.BRICKS, 300);
    storage.set(Resource.IRON, 400);

    var production = new ResourceProduction(50);

    when(resourcesService.refreshAndPersist(1L)).thenReturn(storage);
    when(resourcesService.getProduction(1L)).thenReturn(production);

    mvc.perform(get("/resources/refresh").param("villageId", "1").principal(ALICE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.food").value(100))
        .andExpect(jsonPath("$.wood").value(200))
        .andExpect(jsonPath("$.bricks").value(300))
        .andExpect(jsonPath("$.iron").value(400))
        .andExpect(jsonPath("$.foodPerHour").value(50))
        .andExpect(jsonPath("$.woodPerHour").value(50))
        .andExpect(jsonPath("$.bricksPerHour").value(50))
        .andExpect(jsonPath("$.ironPerHour").value(50));
  }

  @Test
  void refresh_checksOwnershipWithTheAuthenticatedUsername() throws Exception {
    when(resourcesService.refreshAndPersist(1L)).thenReturn(new ResourceStorage());
    when(resourcesService.getProduction(1L)).thenReturn(new ResourceProduction(0));

    mvc.perform(get("/resources/refresh").param("villageId", "1").principal(ALICE))
        .andExpect(status().isOk());

    verify(villageOwnershipService).requireOwner(1L, "alice");
  }

  @Test
  void refresh_notTheOwner_returns403AndNeverTouchesTheVillage() throws Exception {
    doThrow(new AccessDeniedException("Not the owner of village 7"))
        .when(villageOwnershipService)
        .requireOwner(7L, "alice");

    mvc.perform(get("/resources/refresh").param("villageId", "7").principal(ALICE))
        .andExpect(status().isForbidden());

    verify(resourcesService, never()).refreshAndPersist(anyLong());
    verify(resourcesService, never()).getProduction(anyLong());
  }

  @Test
  void refresh_unknownVillage_returns400() throws Exception {
    doThrow(new IllegalArgumentException("Village not found: 99"))
        .when(villageOwnershipService)
        .requireOwner(99L, "alice");

    mvc.perform(get("/resources/refresh").param("villageId", "99").principal(ALICE))
        .andExpect(status().isBadRequest());
  }
}
