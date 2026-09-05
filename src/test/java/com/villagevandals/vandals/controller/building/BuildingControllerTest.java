package com.villagevandals.vandals.controller.building;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.building.BuildingController;
import com.villagevandals.vandals.building.BuildingService;
import com.villagevandals.vandals.web.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full MVC-slice coverage of {@code /building}. Unlike the standalone-setup
 * {@code com.villagevandals.vandals.building.BuildingControllerTest}, this exercises the real
 * dispatcher wiring, so it is where the auto-discovered
 * {@link com.villagevandals.vandals.web.GlobalExceptionHandler} is verified to be picked up without
 * being registered by hand.
 */
@WebMvcTest(BuildingController.class)
class BuildingControllerTest {

  @MockitoBean private BuildingService buildingService;

  @Autowired private MockMvc mvc;

  @MockitoBean private JwtService jwtService;

  @Test
  void shouldReturn200okWhenFetchingAllBuildings() throws Exception {
    mvc.perform(get("/building?villageId=1").with(user("testUser").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(content().string("[]"))
        .andExpect(content().contentType("application/json"));
  }

  @Test
  void shouldReturn200WhenCreatingBuilding() throws Exception {
    mvc.perform(
            post("/building")
                .with(csrf())
                .with(user("testUser").roles("USER"))
                .content(createBuildingJsonRequest())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message.message").value("Constructed building lumbermill successfully"));
  }

  @Test
  void shouldReturn400AndTheReasonWhenTheRequestIsInvalid() throws Exception {
    doThrow(new IllegalArgumentException("Insufficient wood: need 60, have 10"))
        .when(buildingService)
        .constructBuilding(any(), any());

    mvc.perform(
            post("/building")
                .with(csrf())
                .with(user("testUser").roles("USER"))
                .content(createBuildingJsonRequest())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message.message").value("Insufficient wood: need 60, have 10"));
  }

  @Test
  void shouldReturn403WhenConstructingInAVillageTheCallerDoesNotOwn() throws Exception {
    doThrow(new AccessDeniedException("Not the owner of village 1"))
        .when(buildingService)
        .constructBuilding(any(), any());

    mvc.perform(
            post("/building")
                .with(csrf())
                .with(user("testUser").roles("USER"))
                .content(createBuildingJsonRequest())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  private String createBuildingJsonRequest() {
    return """
            {
              "type":"lumbermill",
              "constructionSiteId":1,
              "villageId":1
            }
            """;
  }
}
