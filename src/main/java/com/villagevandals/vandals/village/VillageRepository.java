package com.villagevandals.vandals.village;

import com.villagevandals.vandals.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {

  @Modifying
  @Query(
      value =
"""
    UPDATE village
    SET wood_per_hour = wood_per_hour + :delta
    WHERE id = :villageId
""",
      nativeQuery = true)
  void increaseWoodProduction(@Param("villageId") Long villageId, @Param("delta") int delta);

  Optional<Village> findByOwner(User owner);

  @Modifying
  @Query(
      value =
          """
                        UPDATE village
                        SET food_per_hour = food_per_hour + :delta
                        WHERE id = :villageId
                    """,
      nativeQuery = true)
  void increaseFoodProduction(@Param("villageId") Long villageId, @Param("delta") int delta);
}
