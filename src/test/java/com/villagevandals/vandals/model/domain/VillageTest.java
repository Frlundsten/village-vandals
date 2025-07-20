package com.villagevandals.vandals.model.domain;

import static com.villagevandals.vandals.model.domain.Village.initStarterVillage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;

class VillageTest {

  @Test
  void shouldReturnStarterVillage() {
    assertThatNoException().isThrownBy(() -> initStarterVillage(new User()));
  }

  @Test
  void shouldReturnVillage() {
    int X_COORDINATE = 1;
    int Y_COORDINATE = 2;
    Village v = new Village(X_COORDINATE, Y_COORDINATE, new User());

    assertThat(v).isNotNull();
    assertThat(v.getxCoordinate()).isEqualTo(X_COORDINATE);
    assertThat(v.getyCoordinate()).isEqualTo(Y_COORDINATE);
    assertThat(v.getStorage()).isNotNull();
    assertThat(v.getProduction()).isNotNull();
  }

}
