package com.villagevandals.vandals.village.dto;

import com.villagevandals.vandals.resource.ResourceProduction;
import com.villagevandals.vandals.resource.ResourceStorage;

public record VillageDTO(long id, int xCoordinate, int yCoordinate, ResourceStorage storage, ResourceProduction production) {}
