package com.villagevandals.vandals.model.domain.buildings;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LumberMillTest {

    @Test
    void testUpgradeIncrementLevel(){
        LumberMill lumberMill = new LumberMill();
        lumberMill.upgrade();
        assertThat(lumberMill.getLevel()).isEqualTo(2);
    }

    @Test
    void testGetCost(){
        LumberMill lumberMill = new LumberMill();
        Map<String,Integer> cost = lumberMill.getUpgradeCost();

        assertThat(cost.get("wood")).isEqualTo(200);
        assertThat(cost.get("bricks")).isEqualTo(200);
        assertThat(cost.get("food")).isEqualTo(200);
        assertThat(cost.get("iron")).isEqualTo(200);
    }
}
