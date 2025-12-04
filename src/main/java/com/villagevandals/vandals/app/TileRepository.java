package com.villagevandals.vandals.app;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TileRepository extends JpaRepository<Tile, Long> {

  @Query(
      value = "SELECT * FROM map_tiles WHERE occupied = false ORDER BY RANDOM() LIMIT 1",
      nativeQuery = true)
  Optional<Tile> findRandomFreeTile();

  @Modifying
  @Query("UPDATE Tile t SET t.occupied = true WHERE t.id = :id")
  void markOccupied(Long id);
}
