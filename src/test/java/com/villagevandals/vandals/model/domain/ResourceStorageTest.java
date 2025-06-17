package com.villagevandals.vandals.model.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceStorageTest {

    @Test
    void createStorage(){
    assertThat(new ResourceStorage()).isNotNull();
    }
}
