package com.villagevandals.vandals.village;

import static com.villagevandals.vandals.village.Village.initStarterVillage;

import com.villagevandals.vandals.app.Tile;
import com.villagevandals.vandals.app.TileRepository;
import com.villagevandals.vandals.user.User;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VillageService {
  TileRepository tileRepository;
  VillageRepository villageRepository;

  public VillageService(VillageRepository villageRepository, TileRepository tileRepository) {
    this.villageRepository = villageRepository;
    this.tileRepository = tileRepository;
  }

  /**
   * Claims a random free map tile and initialises a starter village for the user on it.
   * The tile is marked occupied atomically so concurrent registrations cannot claim the same tile.
   *
   * @throws RuntimeException if no free tiles are available
   */
  @Transactional
  public Village starterVillage(User user) {
    Tile tile =
        tileRepository
            .findRandomFreeTile()
            .orElseThrow(() -> new RuntimeException("No free tiles available"));
    tileRepository.markOccupied(tile.getId());

      return initStarterVillage(user, tile);
  }

  /**
   * Returns the village owned by {@code owner}, or empty if none has been created yet.
   */
  public Optional<Village> getStarterVillage(User owner) {
    return villageRepository.findByOwner(owner);
  }
}
