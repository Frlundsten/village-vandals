package com.villagevandals.vandals.village;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single authorization check for village-scoped operations.
 *
 * <p>Every endpoint that reads or mutates village state is addressed by a client-supplied
 * {@code villageId}, and {@code GET /user/all} publishes every village id to every authenticated
 * user, so the id alone proves nothing. Services call {@link #requireOwner} before touching any
 * village state.
 *
 * <p>Ownership is resolved with an existence projection rather than by loading the {@link Village}
 * and dereferencing its lazy {@code owner} association, which would need an open session and would
 * pull in the owner's whole village collection.
 */
@Service
public class VillageOwnershipService {

  private final VillageRepository villageRepository;

  public VillageOwnershipService(VillageRepository villageRepository) {
    this.villageRepository = villageRepository;
  }

  /**
   * Verifies that {@code username} owns the village.
   *
   * @throws AccessDeniedException if there is no authenticated username, or the village exists but
   *     belongs to someone else
   * @throws IllegalArgumentException if no village with that id exists
   */
  @Transactional(readOnly = true)
  public void requireOwner(long villageId, String username) {
    if (username == null || username.isBlank()) {
      throw new AccessDeniedException("No authenticated user for village " + villageId);
    }

    if (villageRepository.existsByIdAndOwner_Username(villageId, username)) {
      return;
    }

    // Only reached on failure, so the happy path stays a single query.
    if (!villageRepository.existsById(villageId)) {
      throw new IllegalArgumentException("Village not found: " + villageId);
    }

    throw new AccessDeniedException("Not the owner of village " + villageId);
  }
}
