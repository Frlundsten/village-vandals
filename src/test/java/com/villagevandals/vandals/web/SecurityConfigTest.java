package com.villagevandals.vandals.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class SecurityConfigTest {
  TestRestTemplate restTemplate;
  URL base;
  @LocalServerPort int port;

  @BeforeEach
  void setUp() throws Exception {
    restTemplate = new TestRestTemplate("user", "password");
    base = new URL("http://localhost:" + port + "/");
  }

  @Test
  public void whenLoggedUserRequestsHomePage_ThenSuccess()
      throws IllegalStateException, IOException {
    ResponseEntity<String> response = restTemplate.getForEntity(base.toString(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("Village");
  }

  @Test
  public void whenUserWithWrongCredentials_thenUnauthorizedPage() throws Exception {

    restTemplate = new TestRestTemplate("user", "wrongpassword");
    ResponseEntity<String> response = restTemplate.getForEntity(base.toString(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("Unauthorized");
  }
}
