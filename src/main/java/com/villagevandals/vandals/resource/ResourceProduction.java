package com.villagevandals.vandals.resource;

import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceProduction {
  private int woodPerHour = 0;
  private int bricksPerHour = 0;
  private int ironPerHour = 0;
  private int foodPerHour = 0;

  public int getWoodPerHour() {
    return woodPerHour;
  }

  public void setWoodPerHour(int woodPerHour) {
    this.woodPerHour = woodPerHour;
  }

  public int getBricksPerHour() {
    return bricksPerHour;
  }

  public void setBricksPerHour(int bricksPerHour) {
    this.bricksPerHour = bricksPerHour;
  }

  public int getIronPerHour() {
    return ironPerHour;
  }

  public void setIronPerHour(int ironPerHour) {
    this.ironPerHour = ironPerHour;
  }

  public int getFoodPerHour() {
    return foodPerHour;
  }

  public void setFoodPerHour(int foodPerHour) {
    this.foodPerHour = foodPerHour;
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
