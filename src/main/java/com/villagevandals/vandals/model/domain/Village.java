package com.villagevandals.vandals.model.domain;

public class Village {
  private int xCoordinate;
  private int yCoordinate;
  private User owner;

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

  public static class Builder {
    private User owner;
    private int x;
    private int y;

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

    public Village build() {
      if (owner == null) {
        throw new IllegalStateException("Owner must be set");
      }
      return new Village(this);
    }
  }
}
