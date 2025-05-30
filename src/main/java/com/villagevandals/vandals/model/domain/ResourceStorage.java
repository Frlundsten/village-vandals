package com.villagevandals.vandals.model.domain;

import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class ResourceStorage {
    private int wood;
    private int stone;
    private int iron;
    private int crop;
    private Instant lastUpdate;
}
