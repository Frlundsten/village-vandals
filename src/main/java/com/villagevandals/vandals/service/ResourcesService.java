package com.villagevandals.vandals.service;

import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.village.VillageRepository;

public class ResourcesService {
    VillageRepository repository;

    public ResourcesService(VillageRepository repository) {
        this.repository = repository;
    }



}
