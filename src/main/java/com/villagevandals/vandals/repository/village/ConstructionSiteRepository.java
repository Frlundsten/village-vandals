package com.villagevandals.vandals.repository.village;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, Long> {
  List<ConstructionSite> findByVillageResourceId(Long villageResourceId);
  Optional<ConstructionSite> findByIdAndVillageResourceId(Long id, Long villageResourceId);
  List<ConstructionSite> getConstructionSiteById(int id);
}
