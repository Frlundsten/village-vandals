package com.villagevandals.vandals.resource;

public record ResourceStorageResponse(
    int food,
    int wood,
    int bricks,
    int iron,
    int foodPerHour,
    int woodPerHour,
    int bricksPerHour,
    int ironPerHour) {}
