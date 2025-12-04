package com.villagevandals.vandals.constructionsite;

import com.villagevandals.vandals.building.buildings.Building;
import com.villagevandals.vandals.village.Village;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "village_id")
  private Village village;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "building_id", referencedColumnName = "id", unique = true)
  private Building building;

  @Column(name = "village_site_id")
  private int villageSiteId;

  public ConstructionSite() {}

  public ConstructionSite(Village village, Building building, int villageSiteId) {
    this.building = building;
    this.village = village;
    this.villageSiteId = villageSiteId;
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

  public void setVillage(Village village) {
    this.village = village;
  }

  public long getVillageSiteId() {
    return villageSiteId;
  }
}
