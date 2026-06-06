package com.villagevandals.vandals.unit;

import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_DAMAGE;
import static com.villagevandals.vandals.gameconfig.GameDefaults.VANDAL_HP;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VANDAL")
public class Vandal extends VillageUnit {

  public Vandal() {
    setHp(VANDAL_HP);
    setDamage(VANDAL_DAMAGE);
  }

  @Override
  public String getUnitType() {
    return "VANDAL";
  }
}
