package com.villagevandals.vandals.repository.village;

import com.villagevandals.vandals.model.domain.User;
import java.util.Optional;

import com.villagevandals.vandals.model.domain.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {
  Optional<Village> findByOwner(User owner);
}
