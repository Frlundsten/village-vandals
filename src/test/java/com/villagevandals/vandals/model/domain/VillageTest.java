package com.villagevandals.vandals.model.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class VillageTest {

  @Test
  void testCreateVillage() {
    User user = new User("User", "random", new ArrayList<>());
    Village village = Village.builder().startingVillage(user).build();
    user.villages().add(village);

    assertThat(village).isNotNull();
    assertThat(village.getOwner()).isEqualTo(user);
    assertThat(village.getX()).isEqualTo(1);
    assertThat(village.getY()).isEqualTo(1);
  }
}
