package com.villagevandals.vandals.village;

import static java.util.Objects.requireNonNull;

import com.villagevandals.vandals.resource.ResourceProduction;
import com.villagevandals.vandals.resource.ResourceStorage;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.dto.VillageDTO;
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

  public static Village initStarterVillage(User user) {
    return new Village(user);
  }

  /**
   * Creates a new starter village with auto-generated X and Y coordinates.
   *
   * <p>Use this constructor when initializing a player's first village.
   *
   * @param owner the user who will be the owner of the village
   */
  private Village(User owner) {
    this.xCoordinate = generateStartingX();
    this.yCoordinate = generateStartingY();
    this.storage = new ResourceStorage();
    this.production = new ResourceProduction();
    this.owner = requireNonNull(owner, "Must have valid owner");
  }

  /**
   * Creates a new village at the specified coordinates with the given user as the owner.
   *
   * @param xCoordinate the x-coordinate where the village will be created
   * @param yCoordinate the y-coordinate where the village will be created
   * @param owner the user who will be set as the owner of the new village
   */
  public Village(int xCoordinate, int yCoordinate, User owner) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.storage = new ResourceStorage();
    this.production = new ResourceProduction();
    this.owner = requireNonNull(owner, "Must have valid owner");
  }

  private int generateStartingX() {
    return 1;
  }

  private int generateStartingY() {
    return 1;
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

  public int getxCoordinate() {
    return xCoordinate;
  }

  public int getyCoordinate() {
    return yCoordinate;
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
