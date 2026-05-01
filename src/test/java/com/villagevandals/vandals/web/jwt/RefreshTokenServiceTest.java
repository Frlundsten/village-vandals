package com.villagevandals.vandals.web.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RefreshTokenServiceTest {

  @Mock RefreshTokenRepository repository;
  @InjectMocks RefreshTokenService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void revokeByUsername_delegatesToRepository() {
    service.revokeByUsername("alice");
    verify(repository).deleteByUsername("alice");
  }

  @Test
  void validateRefreshToken_expired_deletesTokenAndThrows() {
    RefreshToken expired = new RefreshToken();
    expired.setToken("tok");
    expired.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
    when(repository.findByToken("tok")).thenReturn(Optional.of(expired));

    assertThrows(IllegalStateException.class, () -> service.validateRefreshToken("tok"));
    verify(repository).delete(expired);
  }

  @Test
  void validateRefreshToken_valid_returnsToken() {
    RefreshToken valid = new RefreshToken();
    valid.setToken("tok");
    valid.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
    when(repository.findByToken("tok")).thenReturn(Optional.of(valid));

    RefreshToken result = service.validateRefreshToken("tok");

    assertThat(result).isSameAs(valid);
  }

  @Test
  void validateRefreshToken_notFound_throws() {
    when(repository.findByToken("missing")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.validateRefreshToken("missing"));
  }

  @Test
  void createRefreshToken_savesAndReturnsToken() {
    RefreshToken saved = new RefreshToken();
    saved.setToken("generated-uuid");
    when(repository.save(any())).thenReturn(saved);

    RefreshToken result = service.createRefreshToken("alice");

    assertThat(result.getToken()).isEqualTo("generated-uuid");
    verify(repository).save(any());
  }
}
