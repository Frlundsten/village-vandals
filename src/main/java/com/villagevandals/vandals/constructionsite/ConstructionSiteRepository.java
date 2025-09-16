package com.villagevandals.vandals.constructionsite;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, Long> {
 List<ConstructionSite> findByVillageId(Long villageId);

    @Query("SELECT cs FROM ConstructionSite cs " +
            "WHERE cs.id = :id AND cs.village.id = :villageId")
 Optional<ConstructionSite> findByIdAndVillageId(Long id, Long villageId);
}
