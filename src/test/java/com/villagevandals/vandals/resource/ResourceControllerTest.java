package com.villagevandals.vandals.resource;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResourceControllerTest {

  MockMvc mvc;

  @Mock ResourcesService resourcesService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(new ResourceController(resourcesService)).build();
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

    mvc.perform(get("/resources/refresh").param("villageId", "1"))
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
}
