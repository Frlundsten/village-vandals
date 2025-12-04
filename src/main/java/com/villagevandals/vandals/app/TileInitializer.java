package com.villagevandals.vandals.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populate the "world map" table with row and col coordinates. This is then used to randomize
 * starting villages based on if tile is occupied or not.
 */
@Component
public class TileInitializer implements CommandLineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(TileInitializer.class);

  private final TileRepository tileRepository;

  public TileInitializer(TileRepository tileRepository) {
    this.tileRepository = tileRepository;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    // Check if the table is empty
    if (tileRepository.count() > 0) {
      LOG.debug("Tiles already exist. Skipping initialization.");
      return;
    }

    int mapSize = 10;

    for (int row = 0; row < mapSize; row++) {
      for (int col = 0; col < mapSize; col++) {
        Tile tile = new Tile();
        tile.setRow(row);
        tile.setCol(col);
        tile.setOccupied(false);
        tileRepository.save(tile);
      }
    }

    LOG.debug("All tiles initialized!");
  }
}
