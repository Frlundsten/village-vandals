package com.villagevandals.vandals.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.villagevandals.vandals.user.UserService;
import com.villagevandals.vandals.web.jwt.JwtService;
import com.villagevandals.vandals.web.jwt.RefreshToken;
import com.villagevandals.vandals.web.jwt.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

class AuthControllerTest {

  MockMvc mvc;

  @Mock UserService userService;
  @Mock JwtService jwtService;
  @Mock RefreshTokenService refreshTokenService;
  @Mock RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    AuthController controller =
        new AuthController(jwtService, refreshTokenService, userService, restTemplate);
    ReflectionTestUtils.setField(controller, "keycloakBaseUrl", "http://localhost:8080");
    ReflectionTestUtils.setField(controller, "secureCookie", false);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void callback_newUser_provisionsUserAndReturnsInternalToken() throws Exception {
    String fakeIdToken = buildFakeIdToken("alice", "alice@test.com");
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("id_token", fakeIdToken)));

    when(jwtService.generateTokenWithUsername("alice")).thenReturn("internal-jwt");
    RefreshToken rt = new RefreshToken();
    rt.setToken("refresh-uuid");
    when(refreshTokenService.createRefreshToken("alice")).thenReturn(rt);

    mvc.perform(
            post("/auth/callback")
                .contentType(APPLICATION_JSON)
                .content("{\"code\":\"kc-code\",\"redirectUri\":\"http://localhost:5173/auth\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("internal-jwt"));

    verify(userService).provisionKeycloakUser("alice", "alice@test.com");
  }

  @Test
  void callback_existingUser_stillReturnsToken() throws Exception {
    String fakeIdToken = buildFakeIdToken("bob", "bob@test.com");
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("id_token", fakeIdToken)));

    when(jwtService.generateTokenWithUsername("bob")).thenReturn("bob-jwt");
    RefreshToken rt = new RefreshToken();
    rt.setToken("bob-refresh");
    when(refreshTokenService.createRefreshToken("bob")).thenReturn(rt);

    mvc.perform(
            post("/auth/callback")
                .contentType(APPLICATION_JSON)
                .content("{\"code\":\"kc-code\",\"redirectUri\":\"http://localhost:5173/auth\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("bob-jwt"));

    verify(userService).provisionKeycloakUser("bob", "bob@test.com");
  }

  @Test
  void callback_missingCode_returnsBadRequest() throws Exception {
    mvc.perform(
            post("/auth/callback")
                .contentType(APPLICATION_JSON)
                .content("{\"redirectUri\":\"http://localhost:5173/auth\"}"))
        .andExpect(status().isBadRequest());

    verify(restTemplate, never()).postForEntity(anyString(), any(), any());
  }

  @Test
  void callback_missingRedirectUri_returnsBadRequest() throws Exception {
    mvc.perform(
            post("/auth/callback")
                .contentType(APPLICATION_JSON)
                .content("{\"code\":\"kc-code\"}"))
        .andExpect(status().isBadRequest());

    verify(restTemplate, never()).postForEntity(anyString(), any(), any());
  }

  @Test
  void callback_keycloakLogin_returnsKeycloakIdTokenAlongWithAccessToken() throws Exception {
    String fakeIdToken = buildFakeIdToken("alice", "alice@test.com");
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("id_token", fakeIdToken)));
    when(jwtService.generateTokenWithUsername("alice")).thenReturn("internal-jwt");
    RefreshToken rt = new RefreshToken();
    rt.setToken("refresh-uuid");
    when(refreshTokenService.createRefreshToken("alice")).thenReturn(rt);

    mvc.perform(
            post("/auth/callback")
                .contentType(APPLICATION_JSON)
                .content("{\"code\":\"kc-code\",\"redirectUri\":\"http://localhost:5173/auth\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("internal-jwt"))
        .andExpect(jsonPath("$.keycloakIdToken").value(fakeIdToken));
  }

  @Test
  void logout_revokesRefreshTokenAndClearsCookie() throws Exception {
    doNothing().when(refreshTokenService).revokeByUsername("alice");

    mvc.perform(post("/auth/logout").principal(() -> "alice"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("refreshToken", 0));

    verify(refreshTokenService).revokeByUsername("alice");
  }

  @Test
  void logout_keycloakUserSession_isFullyRevoked() throws Exception {
    doNothing().when(refreshTokenService).revokeByUsername("keycloakUser");

    mvc.perform(post("/auth/logout").principal(() -> "keycloakUser"))
        .andExpect(status().isOk());

    verify(refreshTokenService).revokeByUsername("keycloakUser");
  }

  @Test
  void login_localPasswordEndpoint_isRemoved() throws Exception {
    mvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void generateToken_localPasswordEndpoint_isRemoved() throws Exception {
    mvc.perform(post("/auth/generateToken").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isNotFound());
  }

  private String buildFakeIdToken(String username, String email) {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"typ\":\"JWT\",\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"preferred_username\":\"" + username + "\",\"email\":\"" + email + "\"}")
                    .getBytes(StandardCharsets.UTF_8));
    return header + "." + payload + ".";
  }
}
