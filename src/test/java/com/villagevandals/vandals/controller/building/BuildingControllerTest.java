package com.villagevandals.vandals.controller.building;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.service.user.UserInfoService;
import com.villagevandals.vandals.service.village.BuildingService;
import com.villagevandals.vandals.web.jwt.JwtService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BuildingController.class)
class BuildingControllerTest {

  @MockitoBean private BuildingService buildingService;

  @Autowired private MockMvc mvc;

  @MockitoBean private UserInfoService userInfoService;
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
        .andExpect(content().string(""));
  }

  private static Stream<Arguments> errors() {
    return Stream.of(
        arguments(new NullPointerException()), arguments(new IllegalArgumentException()));
  }

  @ParameterizedTest
  @MethodSource("errors")
  void shouldReturn400AndMessageWhenFailedToProcessRequest(Throwable throwable) throws Exception {
    doThrow(throwable).when(buildingService).constructBuilding(any());
    mvc.perform(
            post("/building")
                .with(csrf())
                .with(user("testUser").roles("USER"))
                .content(createBuildingJsonRequest())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Unable to construct building"));
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
