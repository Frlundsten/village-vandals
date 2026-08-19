package com.villagevandals.vandals.unit;

import java.time.Instant;

public record TrainingOrderDTO(
    long id,
    String unitType,
    long buildingId,
    Instant finishesAt,
    int quantity,
    int queuePosition,
    Instant serverTime) {}
