package com.villagevandals.vandals.model.domain;

public class Village {
  private final int xCoordinate;
  private final int yCoordinate;
  private final User owner;
  private ResourceStorage storage;
  private ResourceProduction production;

  public Village(Builder builder) {
    this.xCoordinate = builder.x;
    this.yCoordinate = builder.y;
    this.owner = builder.owner;
  }

  public Village(int xCoordinate, int yCoordinate, User owner) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.owner = owner;
  }

  public Village(int xCoordinate, int yCoordinate, User owner, ResourceStorage storage, ResourceProduction production) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.owner = owner;
    this.storage = storage;
    this.production = production;
  }

  public static Builder builder() {
    return new Builder();
  }

  public User getOwner() {
    return owner;
  }

  public int getX() {
    return xCoordinate;
  }

  public int getY() {
    return yCoordinate;
  }

  public ResourceStorage getStorage() {
    return storage;
  }

  public ResourceProduction getProduction() {
    return production;
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
}
