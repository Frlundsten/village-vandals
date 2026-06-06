package com.villagevandals.vandals.user;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

  MockMvc mvc;

  @Mock UserService userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
  }

  @Test
  void register_localPasswordEndpoint_isRemoved() throws Exception {
    mvc.perform(
            post("/user/register")
                .contentType(APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"secret\"}"))
        .andExpect(status().isNotFound());
  }
}
