package com.villagevandals.vandals.controller.village;

import com.villagevandals.vandals.model.domain.ResourceProduction;
import com.villagevandals.vandals.model.domain.ResourceStorage;

public record VillageDTO(long id, int xCoordinate, int yCoordinate, ResourceStorage storage, ResourceProduction production) {}
