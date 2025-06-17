package com.villagevandals.vandals.model.domain.buildings;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LUMBER_MILL")
public class LumberMill extends Building {
    @Override
    public int productionPerHour() {
        return 20 * getLevel();
    }
}
