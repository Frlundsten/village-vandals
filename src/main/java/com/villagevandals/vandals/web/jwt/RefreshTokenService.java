package com.villagevandals.vandals.web.jwt;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {

  private static final long REFRESH_EXPIRATION_DAYS = 1;

  private final RefreshTokenRepository repository;

  public RefreshTokenService(RefreshTokenRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates and persists a new refresh token for the given user, valid for {@value REFRESH_EXPIRATION_DAYS} day.
   */
  public RefreshToken createRefreshToken(String username) {
    RefreshToken token = new RefreshToken();
    token.setUsername(username);
    token.setToken(UUID.randomUUID().toString());
    token.setExpiryDate(Instant.now().plus(REFRESH_EXPIRATION_DAYS, ChronoUnit.DAYS));
    return repository.save(token);
  }

    /**
     * Looks up a refresh token by value and verifies it has not expired.
     * Deletes the token before throwing if it is expired, preventing any reuse.
     *
     * @throws IllegalArgumentException if the token is not found
     * @throws IllegalStateException if the token has expired
     */
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token =
                repository.findByToken(tokenValue)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Invalid refresh token"));
        if (token.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(token);
            throw new IllegalStateException("Refresh token expired");
        }
        return token;
    }

    /**
     * Deletes all refresh tokens for the given user. Called on logout to prevent
     * any outstanding tokens from being used after the session ends.
     */
    public void revokeByUsername(String username) {
        repository.deleteByUsername(username);
    }

    /**
     * Deletes a single refresh token. Used during token rotation to invalidate
     * the token that was just exchanged for a new one.
     */
    public void revoke(RefreshToken token) {
        repository.delete(token);
    }
}
