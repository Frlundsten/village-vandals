package com.villagevandals.vandals.model.domain;

import com.villagevandals.vandals.controller.village.VillageDTO;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Village {
  public Village() {}

  public Village(int xCoordinate, int yCoordinate, User owner) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.storage = new ResourceStorage();
    this.production = new ResourceProduction();
    this.owner = owner;
  }

  public Village(Builder builder) {
    this.xCoordinate = builder.x;
    this.yCoordinate = builder.y;
    this.owner = builder.owner;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int xCoordinate;
  private int yCoordinate;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User owner;

  @Embedded private ResourceStorage storage;

  @Embedded private ResourceProduction production;

  public Village(
      int xCoordinate,
      int yCoordinate,
      User owner,
      ResourceStorage storage,
      ResourceProduction production) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.owner = owner;
    this.storage = storage;
    this.production = production;
  }

  public ResourceProduction getProduction() {
    return production;
  }

  public ResourceStorage getStorage() {
    return storage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getX() {
    return xCoordinate;
  }

  public int getY() {
    return yCoordinate;
  }

  public static class Builder {
    private User owner;
    private int x;
    private int y;
    private ResourceStorage storage = new ResourceStorage();
    private ResourceProduction production = new ResourceProduction();

    public Builder startingVillage(User owner) {
      this.owner = owner;
      this.x = generateStartingX();
      this.y = generateStartingY();
      return this;
    }

    private int generateStartingX() {
      return 1;
    }

    private int generateStartingY() {
      return 1;
    }

    public Builder storage(ResourceStorage storage) {
      this.storage = storage;
      return this;
    }

    public Builder production(ResourceProduction production) {
      this.production = production;
      return this;
    }

    public Village build() {
      if (owner == null) {
        throw new IllegalStateException("Owner must be set");
      }
      Village village = new Village(this);
      village.storage = this.storage;
      village.production = this.production;
      return village;
    }
  }

  @Override
  public String toString() {
    return "VillageResource{"
        + "id="
        + id
        + ", xCoordinate="
        + xCoordinate
        + ", yCoordinate="
        + yCoordinate
        + ", storage="
        + storage
        + ", production="
        + production
        + '}';
  }

  public User getOwner() {
    return owner;
  }

  public Long getId() {
    return id;
  }

  public VillageDTO toDTO() {
    return new VillageDTO(id, xCoordinate, yCoordinate, storage, production);
  }
}
