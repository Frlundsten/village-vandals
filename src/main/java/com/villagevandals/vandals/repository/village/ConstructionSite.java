package com.villagevandals.vandals.repository.village;

import com.villagevandals.vandals.model.domain.buildings.Building;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class ConstructionSite {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne
  @JoinColumn(name = "village_resource_id")
  private VillageResource villageResource;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "building_id", referencedColumnName = "id", unique = true)
  private Building building;

  public ConstructionSite() {}

  public ConstructionSite(VillageResource villageResource, Building building) {
    this.building = building;
    this.villageResource = villageResource;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Building getBuilding() {
    return building;
  }

  public void setBuilding(Building building) {
    this.building = building;
  }

  public VillageResource getVillageResource() {
    return villageResource;
  }

  public void setVillageResource(VillageResource villageResource) {
    this.villageResource = villageResource;
  }
}
