package com.villagevandals.vandals.building.buildings;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_STARTING_LEVEL;
import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_STARTING_RESOURCE_COUNT;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;
import java.util.HashMap;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "building_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Building {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String type;

  @Transient private final int woodCost = DEFAULT_STARTING_RESOURCE_COUNT;
  @Transient private final int bricksCost = DEFAULT_STARTING_RESOURCE_COUNT;
  @Transient private final int foodCost = DEFAULT_STARTING_RESOURCE_COUNT;
  @Transient private final int ironCost = DEFAULT_STARTING_RESOURCE_COUNT;
  private int level = DEFAULT_STARTING_LEVEL;

  public Long getId() {
    return id;
  }

  public void upgrade() {
    level++;
  }

  public Map<String, Integer> getUpgradeCost() {
    if (level == DEFAULT_STARTING_LEVEL) {
      Map<String, Integer> cost = new HashMap<>();
      cost.put("wood", woodCost);
      cost.put("bricks", bricksCost);
      cost.put("food", foodCost);
      cost.put("iron", ironCost);
      return cost;
    }

    Map<String, Integer> cost = new HashMap<>();
    cost.put("wood", woodCost * nextLevel());
    cost.put("bricks", bricksCost * nextLevel());
    cost.put("food", foodCost * nextLevel());
    cost.put("iron", ironCost * nextLevel());
    return cost;
  }

  private int nextLevel() {
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
