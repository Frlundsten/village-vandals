package com.villagevandals.vandals.model.domain;

import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Amount of resources acquired and when it was last updated at db */
@Embeddable
public class ResourceStorage {
  private int wood = 100;
  private int stone = 100;
  private int iron = 100;
  private int crop = 100;
  private Instant lastUpdate = Instant.now();

  public int getWood() {
    return wood;
  }

  public void setWood(int wood) {
    this.wood = wood;
  }

  public int getStone() {
    return stone;
  }

  public void setStone(int stone) {
    this.stone = stone;
  }

  public int getIron() {
    return iron;
  }

  public void setIron(int iron) {
    this.iron = iron;
  }

  public int getCrop() {
    return crop;
  }

  public void setCrop(int crop) {
    this.crop = crop;
  }

  public Instant getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Instant lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  @Override
  public String toString() {
    return "ResourceStorage{" +
            "wood=" + wood +
            ", stone=" + stone +
            ", iron=" + iron +
            ", crop=" + crop +
            ", lastUpdate=" + lastUpdate +
            '}';
  }
}
