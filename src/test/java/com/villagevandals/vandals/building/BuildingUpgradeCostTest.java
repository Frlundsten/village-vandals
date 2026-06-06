package com.villagevandals.vandals.building;

import com.villagevandals.vandals.building.buildings.*;
import com.villagevandals.vandals.resource.Resource;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuildingUpgradeCostTest {

    // --- Level-1 cost assertions for all five buildings ---

    @Test
    void lumberMill_level1_upgradeCost() {
        var cost = new LumberMill().getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(320);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.IRON)).isEqualTo(160);
    }

    @Test
    void brickyard_level1_upgradeCost() {
        var cost = new Brickyard().getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(320);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(160);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.IRON)).isEqualTo(160);
    }

    @Test
    void forge_level1_upgradeCost() {
        var cost = new Forge().getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(320);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(320);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.IRON)).isEqualTo(160);
    }

    @Test
    void farm_level1_upgradeCost() {
        var cost = new Farm().getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(320);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(160);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.IRON)).isEqualTo(160);
    }

    @Test
    void barrack_level1_upgradeCost() {
        var cost = new Barrack().getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(320);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(160);
        assertThat(cost.get(Resource.IRON)).isEqualTo(320);
    }

    // --- Linear scaling: level 2 LumberMill ---

    @Test
    void lumberMill_level2_scalesLinearly() {
        LumberMill lumberMill = new LumberMill();
        lumberMill.upgrade(); // now level 2, nextLevel = 3
        var cost = lumberMill.getUpgradeCostAsResourceMap();
        assertThat(cost.get(Resource.WOOD)).isEqualTo(240);
        assertThat(cost.get(Resource.BRICKS)).isEqualTo(480);
        assertThat(cost.get(Resource.FOOD)).isEqualTo(240);
        assertThat(cost.get(Resource.IRON)).isEqualTo(240);
    }

    // --- Consistency: getUpgradeCost() string map matches getUpgradeCostAsResourceMap() ---

    @Test
    void lumberMill_stringMapMatchesResourceMap() {
        assertMapsConsistent(new LumberMill());
    }

    @Test
    void brickyard_stringMapMatchesResourceMap() {
        assertMapsConsistent(new Brickyard());
    }

    @Test
    void forge_stringMapMatchesResourceMap() {
        assertMapsConsistent(new Forge());
    }

    @Test
    void farm_stringMapMatchesResourceMap() {
        assertMapsConsistent(new Farm());
    }

    @Test
    void barrack_stringMapMatchesResourceMap() {
        assertMapsConsistent(new Barrack());
    }

    private void assertMapsConsistent(Building building) {
        Map<Resource, Integer> resourceMap = building.getUpgradeCostAsResourceMap();
        Map<String, Integer> stringMap = building.getUpgradeCost();
        assertThat(stringMap.get("wood")).isEqualTo(resourceMap.get(Resource.WOOD));
        assertThat(stringMap.get("bricks")).isEqualTo(resourceMap.get(Resource.BRICKS));
        assertThat(stringMap.get("food")).isEqualTo(resourceMap.get(Resource.FOOD));
        assertThat(stringMap.get("iron")).isEqualTo(resourceMap.get(Resource.IRON));
    }
}
