package com.villagevandals.vandals.model.domain;

import static com.villagevandals.vandals.village.Village.initStarterVillage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.villagevandals.vandals.app.Tile;
import com.villagevandals.vandals.user.User;
import com.villagevandals.vandals.village.Village;
import org.junit.jupiter.api.Test;

class VillageTest {

  @Test
  void shouldReturnStarterVillage() {
    assertThatNoException().isThrownBy(() -> initStarterVillage(new User(), new Tile()));
  }

  @Test
  void shouldReturnVillage() {
    int X_COORDINATE = 1;
    int Y_COORDINATE = 2;
    Village v = new Village(X_COORDINATE, Y_COORDINATE, new User());

    assertThat(v).isNotNull();
    assertThat(v.getXCoordinate()).isEqualTo(X_COORDINATE);
    assertThat(v.getYCoordinate()).isEqualTo(Y_COORDINATE);
    assertThat(v.getStorage()).isNotNull();
    assertThat(v.getProduction()).isNotNull();
  }

}
