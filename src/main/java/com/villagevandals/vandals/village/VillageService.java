package com.villagevandals.vandals.village;

import static com.villagevandals.vandals.village.Village.initStarterVillage;

import com.villagevandals.vandals.user.User;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class VillageService {

  VillageRepository villageRepository;

  public VillageService(VillageRepository villageRepository) {
    this.villageRepository = villageRepository;
  }

  public Village starterVillage(User user) {
    return initStarterVillage(user);
  }

  public Optional<Village> getStarterVillage(User owner) {
    return villageRepository.findByOwner(owner);
  }
}
