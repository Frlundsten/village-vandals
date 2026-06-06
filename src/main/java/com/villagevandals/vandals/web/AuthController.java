package com.villagevandals.vandals.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagevandals.vandals.user.UserService;
import com.villagevandals.vandals.web.jwt.JwtService;
import com.villagevandals.vandals.web.jwt.RefreshToken;
import com.villagevandals.vandals.web.jwt.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;
  private final RestTemplate restTemplate;

  @Value("${keycloak.base-url}")
  private String keycloakBaseUrl;

  @Value("${keycloak.client-id}")
  private String keycloakClientId;

  @Value("${keycloak.client-secret}")
  private String keycloakClientSecret;

  @Value("${app.secure-cookie:false}")
  private boolean secureCookie;

  AuthController(
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      UserService userService,
      RestTemplate restTemplate) {
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.userService = userService;
    this.restTemplate = restTemplate;
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String refreshTokenValue =
        Arrays.stream(cookies)
            .filter(c -> "refreshToken".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No refresh token"));

    RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

    refreshTokenService.revoke(refreshToken);
    RefreshToken newRefresh = refreshTokenService.createRefreshToken(refreshToken.getUsername());

    response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(newRefresh.getToken()).toString());

    String accessToken = jwtService.generateTokenWithUsername(refreshToken.getUsername());

    return ResponseEntity.ok(AuthResponse.local(accessToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletResponse response, Principal principal) {
    refreshTokenService.revokeByUsername(principal.getName());

    ResponseCookie cleared =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .path("/auth/refresh")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/callback")
  public ResponseEntity<?> keycloakCallback(
      @RequestBody Map<String, String> body, HttpServletResponse response) {

    String code = body.get("code");
    String redirectUri = body.get("redirectUri");

    if (code == null || redirectUri == null) {
      return ResponseEntity.badRequest().body("Missing code or redirectUri");
    }

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("grant_type", "authorization_code");
      params.add("code", code);
      params.add("client_id", keycloakClientId);
      params.add("client_secret", keycloakClientSecret);
      params.add("redirect_uri", redirectUri);

      ResponseEntity<Map> kcResponse =
          restTemplate.postForEntity(
              keycloakBaseUrl + "/realms/villagevandals/protocol/openid-connect/token",
              new HttpEntity<>(params, headers),
              Map.class);

      Map<String, Object> tokens = kcResponse.getBody();

      String idToken = (String) tokens.get("id_token");
      Map<String, Object> claims = decodeJwtPayload(idToken);
      String username = (String) claims.get("preferred_username");
      String email = (String) claims.get("email");

      userService.provisionKeycloakUser(username, email);

      String accessToken = jwtService.generateTokenWithUsername(username);
      RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

      response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken.getToken()).toString());

      return ResponseEntity.ok(new AuthResponse(accessToken, idToken));

    } catch (Exception e) {
      log.error("Error exchanging Keycloak code for token", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error exchanging code for token: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> decodeJwtPayload(String jwt) throws Exception {
    String[] parts = jwt.split("\\.");
    String padded = parts[1];
    int mod = padded.length() % 4;
    if (mod == 2) padded += "==";
    else if (mod == 3) padded += "=";
    byte[] payloadBytes = Base64.getUrlDecoder().decode(padded);
    return new ObjectMapper().readValue(payloadBytes, Map.class);
  }

  private ResponseCookie buildRefreshCookie(String token) {
    return ResponseCookie.from("refreshToken", token)
        .httpOnly(true)
        .secure(secureCookie)
        .path("/auth/refresh")
        .maxAge(Duration.ofDays(1))
        .sameSite("Strict")
        .build();
  }
}
