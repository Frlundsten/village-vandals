package com.villagevandals.vandals.village;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

class VillageOwnershipServiceTest {

  @Mock VillageRepository villageRepository;

  VillageOwnershipService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new VillageOwnershipService(villageRepository);
  }

  @Test
  void requireOwner_ownerMatches_passes() {
    when(villageRepository.existsByIdAndOwner_Username(1L, "alice")).thenReturn(true);

    assertThatCode(() -> service.requireOwner(1L, "alice")).doesNotThrowAnyException();
  }

  @Test
  void requireOwner_ownerMatches_doesNotIssueASecondQuery() {
    when(villageRepository.existsByIdAndOwner_Username(1L, "alice")).thenReturn(true);

    service.requireOwner(1L, "alice");

    verify(villageRepository, never()).existsById(1L);
  }

  @Test
  void requireOwner_villageOwnedBySomeoneElse_throwsAccessDenied() {
    when(villageRepository.existsByIdAndOwner_Username(1L, "bob")).thenReturn(false);
    when(villageRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.requireOwner(1L, "bob"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void requireOwner_villageDoesNotExist_throwsIllegalArgument() {
    when(villageRepository.existsByIdAndOwner_Username(99L, "alice")).thenReturn(false);
    when(villageRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> service.requireOwner(99L, "alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("99");
  }

  @Test
  void requireOwner_noAuthenticatedUsername_throwsAccessDeniedWithoutQuerying() {
    assertThatThrownBy(() -> service.requireOwner(1L, null))
        .isInstanceOf(AccessDeniedException.class);

    verify(villageRepository, never()).existsByIdAndOwner_Username(1L, null);
  }
}
