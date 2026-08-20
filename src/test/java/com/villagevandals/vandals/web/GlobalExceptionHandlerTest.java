package com.villagevandals.vandals.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  MockMvc mvc;

  @RestController
  static class ThrowingController {
    @GetMapping("/boom/denied")
    String denied() {
      throw new AccessDeniedException("Not the owner of village 1");
    }

    @GetMapping("/boom/bad-argument")
    String badArgument() {
      throw new IllegalArgumentException("Village not found: 99");
    }

    @GetMapping("/boom/bad-state")
    String badState() {
      throw new IllegalStateException("No building to upgrade at this site");
    }

    @GetMapping("/boom/silent")
    String silent() {
      throw new AccessDeniedException(null);
    }
  }

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void accessDenied_mapsToForbidden() throws Exception {
    mvc.perform(get("/boom/denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message.message").value("Not the owner of village 1"));
  }

  @Test
  void illegalArgument_mapsToBadRequest() throws Exception {
    mvc.perform(get("/boom/bad-argument"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message.message").value("Village not found: 99"));
  }

  @Test
  void illegalState_mapsToConflict() throws Exception {
    mvc.perform(get("/boom/bad-state"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message.message").value("No building to upgrade at this site"));
  }

  @Test
  void exceptionWithoutMessage_stillProducesABody() throws Exception {
    mvc.perform(get("/boom/silent"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message.message").exists());
  }
}
