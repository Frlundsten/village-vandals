package com.villagevandals.vandals.service.village;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.village.VillageRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VillageService {

  VillageRepository villageRepository;

  public VillageService(VillageRepository villageRepository) {
    this.villageRepository = villageRepository;
  }

  public Village starterVillage(User user) {
    return Village.builder().startingVillage(user).build();
  }

  public Optional<Village> getStarterVillage(User owner) {
    return villageRepository.findByOwner(owner);
  }
}
