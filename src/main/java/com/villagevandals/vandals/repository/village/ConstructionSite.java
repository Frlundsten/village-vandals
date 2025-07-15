package com.villagevandals.vandals.repository.village;

import com.villagevandals.vandals.model.domain.Village;
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
  private Long id;

  @ManyToOne
  @JoinColumn(name = "village_id")
  private Village village;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "building_id", referencedColumnName = "id", unique = true)
  private Building building;

  public ConstructionSite() {}

  public ConstructionSite(Village village, Building building) {
    this.building = building;
    this.village = village;
  }

  public Long getId() {
    return id;
  }

  public Building getBuilding() {
    return building;
  }

  public void setBuilding(Building building) {
    this.building = building;
  }

  public Village getVillage() {
    return village;
  }

}
