package com.villagevandals.vandals.resource;

import static com.villagevandals.vandals.gameconfig.GameDefaults.DEFAULT_BASE_PRODUCTION_RATE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceProductionTest {

  @Test
  void withDefaults_returnsBaseRateForAllResources() {
    ResourceProduction production = ResourceProduction.withDefaults();

    assertThat(production.getWoodPerHour()).isEqualTo(DEFAULT_BASE_PRODUCTION_RATE);
    assertThat(production.getBricksPerHour()).isEqualTo(DEFAULT_BASE_PRODUCTION_RATE);
    assertThat(production.getIronPerHour()).isEqualTo(DEFAULT_BASE_PRODUCTION_RATE);
    assertThat(production.getFoodPerHour()).isEqualTo(DEFAULT_BASE_PRODUCTION_RATE);
  }
}
