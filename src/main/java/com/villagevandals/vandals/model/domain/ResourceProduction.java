package com.villagevandals.vandals.model.domain;

import static com.villagevandals.vandals.service.util.GameDefaults.DEFAULT_PRODUCTION_PER_HOUR;

import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceProduction {
  private int woodPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int clayPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int ironPerHour = DEFAULT_PRODUCTION_PER_HOUR;
  private int cropPerHour = DEFAULT_PRODUCTION_PER_HOUR;

  public int getWoodPerHour() {
    return woodPerHour;
  }

  public void setWoodPerHour(int woodPerHour) {
    this.woodPerHour = woodPerHour;
  }

  public int getClayPerHour() {
    return clayPerHour;
  }

  public int getIronPerHour() {
    return ironPerHour;
  }

  public int getCropPerHour() {
    return cropPerHour;
  }

  @Override
  public String toString() {
    return "ResourceProduction{"
        + "woodPerHour="
        + woodPerHour
        + ", clayPerHour="
        + clayPerHour
        + ", ironPerHour="
        + ironPerHour
        + ", cropPerHour="
        + cropPerHour
        + '}';
  }
}
