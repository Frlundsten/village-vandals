package com.villagevandals.vandals.unit;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "unit")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "unit_type", discriminatorType = DiscriminatorType.STRING)
public abstract class VillageUnit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int hp;
  private int damage;

  public Long getId() {
    return id;
  }

  public int getHp() {
    return hp;
  }

  protected void setHp(int hp) {
    this.hp = hp;
  }

  public int getDamage() {
    return damage;
  }

  protected void setDamage(int damage) {
    this.damage = damage;
  }

  public abstract String getUnitType();
}
