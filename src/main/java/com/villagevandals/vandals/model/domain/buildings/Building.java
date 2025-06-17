package com.villagevandals.vandals.model.domain.buildings;

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
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "building_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Building {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String type;
  @Transient
  private final int woodCost = 100;
  @Transient
  private final int bricksCost = 100;
  @Transient
  private final int foodCost = 100;
  @Transient
  private final int ironCost = 100;
  private int level = 1;

  public Long getId() {
    return id;
  }

  public abstract int productionPerHour();

  public void upgrade() {
    level++;
  }

  public Map<String, Integer> getUpgradeCost() {
    Map<String, Integer> cost = new HashMap<>();
    cost.put("wood", woodCost * (level + 1));
    cost.put("bricks", bricksCost * (level + 1));
    cost.put("food", foodCost * (level + 1));
    cost.put("iron", ironCost * (level + 1));
    return cost;
  }

  public int getLevel() {
    return level;
  }
}
