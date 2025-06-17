package com.villagevandals.vandals.service;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.user.UserResource;
import com.villagevandals.vandals.repository.village.ConstructionSite;
import com.villagevandals.vandals.repository.village.ConstructionSiteRepository;
import com.villagevandals.vandals.service.user.UserService;
import com.villagevandals.vandals.service.village.VillageService;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

  private final ConstructionSiteRepository constructionSiteRepository;
  UserService userService;
  VillageService villageService;

  public RegistrationService(
      UserService userService,
      VillageService villageService,
      ConstructionSiteRepository constructionSiteRepository) {
    this.userService = userService;
    this.villageService = villageService;
    this.constructionSiteRepository = constructionSiteRepository;
  }

  public void newUser(String username, String rawPassword) {
    User user = userService.newUser(username, rawPassword);
    Village village = villageService.starterVillage(user);

    user.villages().add(village);

    UserResource ur = userService.saveNewUser(user);

    var villageResource = villageService.getStarterVillage(ur);

    System.out.println("Village Resource: " + villageResource);

    if (villageResource.isPresent()) {
      var constructionList = new ArrayList<ConstructionSite>();

      for (int i = 0; i < 8; i++) {

        constructionList.add(new ConstructionSite(villageResource.get(),null));
      }
      constructionSiteRepository.saveAll(constructionList);
    }
  }
}
