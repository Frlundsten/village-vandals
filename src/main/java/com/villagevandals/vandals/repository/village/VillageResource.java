package com.villagevandals.vandals.repository.village;

import com.villagevandals.vandals.model.domain.ResourceProduction;
import com.villagevandals.vandals.model.domain.ResourceStorage;
import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.user.UserResource;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class VillageResource {

  public VillageResource() {}

  public VillageResource(int xCoordinate, int yCoordinate, UserResource owner) {
    this.xCoordinate = xCoordinate;
    this.yCoordinate = yCoordinate;
    this.owner = owner;
    this.storage = new ResourceStorage();
    this.production = new ResourceProduction();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int xCoordinate;
  private int yCoordinate;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private UserResource owner;

  @Embedded
  private ResourceStorage storage;

  @Embedded
  private ResourceProduction production;

  public static Village toVillage(VillageResource resource, User owner){
    return new Village(resource.xCoordinate,resource.yCoordinate,owner);
  }
}
