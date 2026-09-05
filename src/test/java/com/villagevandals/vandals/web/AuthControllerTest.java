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
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  // --- /auth/refresh: an unusable token is the end of a session, not a server fault ---

  @Test
  void refresh_noCookiesAtAll_returns401() throws Exception {
    mvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());

    verify(refreshTokenService, never()).validateRefreshToken(anyString());
  }

  @Test
  void refresh_cookiesButNoRefreshCookie_returns401() throws Exception {
    mvc.perform(post("/auth/refresh").cookie(new Cookie("somethingElse", "value")))
        .andExpect(status().isUnauthorized());

    verify(refreshTokenService, never()).validateRefreshToken(anyString());
  }

  @Test
  void refresh_unknownRefreshToken_returns401AndIssuesNothing() throws Exception {
    when(refreshTokenService.validateRefreshToken("unknown"))
        .thenThrow(new IllegalArgumentException("Invalid refresh token"));

    mvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "unknown")))
        .andExpect(status().isUnauthorized());

    verify(refreshTokenService, never()).createRefreshToken(anyString());
    verify(jwtService, never()).generateTokenWithUsername(anyString());
  }

  @Test
  void refresh_expiredRefreshToken_returns401() throws Exception {
    when(refreshTokenService.validateRefreshToken("expired"))
        .thenThrow(new IllegalStateException("Refresh token expired"));

    mvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "expired")))
        .andExpect(status().isUnauthorized());

    verify(refreshTokenService, never()).createRefreshToken(anyString());
  }

  @Test
  void refresh_validRefreshToken_returnsNewAccessTokenAndRotatesTheCookie() throws Exception {
    RefreshToken current = new RefreshToken();
    current.setToken("current");
    current.setUsername("alice");
    current.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

    RefreshToken rotated = new RefreshToken();
    rotated.setToken("rotated");
    rotated.setUsername("alice");

    when(refreshTokenService.validateRefreshToken("current")).thenReturn(current);
    when(refreshTokenService.createRefreshToken("alice")).thenReturn(rotated);
    when(jwtService.generateTokenWithUsername("alice")).thenReturn("fresh-jwt");

    mvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "current")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("fresh-jwt"))
        .andExpect(cookie().value("refreshToken", "rotated"));

    verify(refreshTokenService).revoke(current);
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
