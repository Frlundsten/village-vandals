package com.villagevandals.vandals.service.village;

import static com.villagevandals.vandals.model.domain.Village.initStarterVillage;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.village.VillageRepository;
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
