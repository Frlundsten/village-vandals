package com.villagevandals.vandals.village;

import com.villagevandals.vandals.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {
  Optional<Village> findByOwner(User owner);

  /**
   * Existence projection used by the ownership check. Resolves ownership in a single count query
   * without materialising the {@link Village} or its lazy {@code owner} association.
   */
  boolean existsByIdAndOwner_Username(Long id, String username);
}
