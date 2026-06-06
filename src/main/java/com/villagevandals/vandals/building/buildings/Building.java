package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_STARTING_LEVEL;

import com.villagevandals.vandals.resource.Resource;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "building_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Building {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String type;

  private int level = DEFAULT_STARTING_LEVEL;

  public Long getId() {
    return id;
  }

  public void upgrade() {
    level++;
  }

  public abstract Map<Resource, Integer> getConstructionCost();

  public abstract Map<Resource, Integer> getUpgradeCostAsResourceMap();

  public Map<String, Integer> getUpgradeCost() {
    Map<Resource, Integer> resourceMap = getUpgradeCostAsResourceMap();
    return Map.of(
        "wood", resourceMap.get(Resource.WOOD),
        "bricks", resourceMap.get(Resource.BRICKS),
        "food", resourceMap.get(Resource.FOOD),
        "iron", resourceMap.get(Resource.IRON));
  }

  protected int nextLevel() {
    return level + 1;
  }

  public int getLevel() {
    return level;
  }

  protected void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
