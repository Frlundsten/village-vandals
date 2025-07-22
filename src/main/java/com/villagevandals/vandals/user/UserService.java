package com.villagevandals.vandals.user;

import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.village.dto.VillageDTO;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.web.UserInfo;
import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.village.VillageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
  UserRepository userRepository;
  PasswordEncoder passwordEncoder;
  VillageService villageService;
  ConstructionSiteRepository constructionSiteRepository;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      VillageService villageService,
      ConstructionSiteRepository constructionSiteRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.villageService = villageService;
    this.constructionSiteRepository = constructionSiteRepository;
  }

  public void newUser(UserInfo userInfo) {
    if (userRepository.findByUsername(userInfo.username()).isPresent()) {
      throw new RuntimeException("Username already taken");
    }

    var user =
        new User(
            UUID.randomUUID(),
            userInfo.username(),
            passwordEncoder.encode(userInfo.password()),
            userInfo.email(),
            userInfo.roles(),
            new ArrayList<>());

    saveUser(setupNewUserVillage(user));

    addConstructionSitesForVillage(user);
  }

  private User setupNewUserVillage(User user) {
    Village village = villageService.starterVillage(user);
    user.addToVillages(village);

    return user;
  }

  private void saveUser(User user) {
    userRepository.save(user);
  }

  private void addConstructionSitesForVillage(User owner) {
    Optional<Village> village = villageService.getStarterVillage(owner);

    if (village.isPresent()) {
      var constructionList = new ArrayList<ConstructionSite>();
      for (int i = 0; i < 11; i++) {
        constructionList.add(new ConstructionSite(village.get(), null));
      }
      constructionSiteRepository.saveAll(constructionList);
    }
  }

  public UserDTO getUserInfo(String username) {
    LOG.debug("Getting user info for username {}", username);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Username not found"));

    List<VillageDTO> villageDTOList = user.getVillages().stream().map(Village::toDTO).toList();

    return new UserDTO(user.getId(), username, villageDTOList);
  }
}
