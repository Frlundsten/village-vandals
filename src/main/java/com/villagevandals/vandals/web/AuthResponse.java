package com.villagevandals.vandals.web;

public record AuthResponse(String accessToken, String keycloakIdToken) {
  public static AuthResponse local(String accessToken) {
    return new AuthResponse(accessToken, null);
  }
}
