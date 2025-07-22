package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_STARTING_RESOURCE_COUNT;

import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Amount of resources acquired and when it was last updated at db */
@Embeddable
public class ResourceStorage {
  private int wood = DEFAULT_STARTING_RESOURCE_COUNT;
  private int bricks = DEFAULT_STARTING_RESOURCE_COUNT;
  private int iron = DEFAULT_STARTING_RESOURCE_COUNT;
  private int food = DEFAULT_STARTING_RESOURCE_COUNT;
  private Instant lastUpdate = Instant.now();

  public int getWood() {
    return wood;
  }

  public void setWood(int wood) {
    this.wood = wood;
  }

  public int getBricks() {
    return bricks;
  }

  public void setBricks(int bricks) {
    this.bricks = bricks;
  }

  public int getIron() {
    return iron;
  }

  public void setIron(int iron) {
    this.iron = iron;
  }

  public int getFood() {
    return food;
  }

  public void setFood(int food) {
    this.food = food;
  }

  public Instant getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Instant lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  @Override
  public String toString() {
    return "ResourceStorage{"
        + "wood="
        + wood
        + ", bricks="
        + bricks
        + ", iron="
        + iron
        + ", food="
        + food
        + ", lastUpdate="
        + lastUpdate
        + '}';
  }
}
