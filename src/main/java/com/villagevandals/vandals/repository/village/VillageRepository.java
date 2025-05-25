package com.villagevandals.vandals.repository.village;

import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.user.UserResource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VillageRepository extends JpaRepository<VillageResource, Long> {
  Optional<VillageResource> findByOwner(UserResource owner);
}
