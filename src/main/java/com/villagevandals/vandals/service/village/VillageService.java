package com.villagevandals.vandals.service.village;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.user.UserResource;
import com.villagevandals.vandals.repository.village.VillageRepository;
import com.villagevandals.vandals.repository.village.VillageResource;
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

  public Optional<VillageResource> getStarterVillage(UserResource ur) {
    return villageRepository.findByOwner(ur);
  }
}
