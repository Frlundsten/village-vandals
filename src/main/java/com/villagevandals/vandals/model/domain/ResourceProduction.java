package com.villagevandals.vandals.model.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class ResourceProduction {
  private int woodPerHour = 3600;
  private int clayPerHour = 3600;
  private int ironPerHour = 3600;
  private int cropPerHour = 3600;

  public int getWoodPerHour() {
    return woodPerHour;
  }

  public void setWoodPerHour(int woodPerHour) {
    this.woodPerHour = woodPerHour;
  }

  public int getClayPerHour() {
    return clayPerHour;
  }

  public void setClayPerHour(int clayPerHour) {
    this.clayPerHour = clayPerHour;
  }

  public int getIronPerHour() {
    return ironPerHour;
  }

  public void setIronPerHour(int ironPerHour) {
    this.ironPerHour = ironPerHour;
  }

  public int getCropPerHour() {
    return cropPerHour;
  }

  public void setCropPerHour(int cropPerHour) {
    this.cropPerHour = cropPerHour;
  }

  @Override
  public String toString() {
    return "ResourceProduction{" +
            "woodPerHour=" + woodPerHour +
            ", clayPerHour=" + clayPerHour +
            ", ironPerHour=" + ironPerHour +
            ", cropPerHour=" + cropPerHour +
            '}';
  }
}
