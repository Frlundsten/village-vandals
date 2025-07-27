package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.util.GameDefaults.DEFAULT_PRODUCTION_PER_HOUR;

import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceProduction {
  private int woodPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int bricksPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int ironPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int foodPerHour = DEFAULT_PRODUCTION_PER_HOUR;

  public int getWoodPerHour() {
    return woodPerHour;
  }

  public void setWoodPerHour(int woodPerHour) {
    this.woodPerHour = woodPerHour;
  }

  public int getBricksPerHour() {
    return bricksPerHour;
  }

  public int getIronPerHour() {
    return ironPerHour;
  }

  public int getFoodPerHour() {
    return foodPerHour;
  }

  @Override
  public String toString() {
    return "ResourceProduction{"
        + "woodPerHour="
        + woodPerHour
        + ", bricksPerHour="
        + bricksPerHour
        + ", ironPerHour="
        + ironPerHour
        + ", foodPerHour="
        + foodPerHour
        + '}';
  }
}
