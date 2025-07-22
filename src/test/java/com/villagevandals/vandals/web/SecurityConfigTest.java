//package com.villagevandals.vandals.web;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.net.URL;
//
//import com.villagevandals.vandals.user.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//
//class SecurityConfigTest {
//  TestRestTemplate restTemplate;
//  URL base;
//  @LocalServerPort int port;
//
//  @MockitoBean
//  private UserRepository userRepository;
//
//  @BeforeEach
//  void setUp() throws Exception {
//    restTemplate = new TestRestTemplate("user", "password");
//    base = new URL("http://localhost:" + port + "/village");
//  }
//
//  @Test
//  public void whenLoggedUserRequestsHomePage_ThenSuccess() throws IllegalStateException {
//    ResponseEntity<String> response = restTemplate.getForEntity(base.toString(), String.class);
//
//    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//    assertThat(response.getBody()).contains("Village");
//  }
//
//  @Test
//  public void whenUserWithWrongCredentials_thenUnauthorizedPage() throws Exception {
//
//    restTemplate = new TestRestTemplate("user", "wrongpassword");
//    ResponseEntity<String> response = restTemplate.getForEntity(base.toString(), String.class);
//
//    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
//    assertThat(response.getBody()).contains("Unauthorized");
//  }
//}
