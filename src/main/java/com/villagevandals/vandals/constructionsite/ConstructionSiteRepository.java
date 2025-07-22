package com.villagevandals.vandals.constructionsite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, Long> {
 List<ConstructionSite> findByVillageId(Long villageId);
 Optional<ConstructionSite> findByIdAndVillageId(Long id, Long villageId);
}
