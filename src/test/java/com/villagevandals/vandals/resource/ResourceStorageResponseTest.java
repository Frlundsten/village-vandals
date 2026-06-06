package com.villagevandals.vandals.resource;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResourceStorageResponseTest {

  @Test
  void holdsAmountsAndProductionRates() {
    var response = new ResourceStorageResponse(10, 20, 30, 40, 100, 200, 300, 400);

    assertThat(response.food()).isEqualTo(10);
    assertThat(response.wood()).isEqualTo(20);
    assertThat(response.bricks()).isEqualTo(30);
    assertThat(response.iron()).isEqualTo(40);
    assertThat(response.foodPerHour()).isEqualTo(100);
    assertThat(response.woodPerHour()).isEqualTo(200);
    assertThat(response.bricksPerHour()).isEqualTo(300);
    assertThat(response.ironPerHour()).isEqualTo(400);
  }
}
