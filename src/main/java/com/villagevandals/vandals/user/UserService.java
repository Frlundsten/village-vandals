package com.villagevandals.vandals.user;

import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.user.dto.UserVillageFlatDTO;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageService;
import com.villagevandals.vandals.village.dto.VillageDTO;
import com.villagevandals.vandals.web.UserInfo;
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
 private final UserRepository userRepository;
 private final PasswordEncoder passwordEncoder;
 private final VillageService villageService;
 private final ConstructionSiteRepository constructionSiteRepository;

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

  /**
   * Registers a new account, creates a starter village, and populates 11 construction sites.
   *
   * @throws RuntimeException if the username or email is already taken
   */
  public void newUser(UserInfo userInfo) {
    if (userRepository.findByUsername(userInfo.username()).isPresent()) {
      throw new RuntimeException("Username already taken");
    }
    if (userRepository.findByEmail(userInfo.email()).isPresent()) {
      throw new RuntimeException("Email already registered");
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
      for (int villageSiteId = 1; villageSiteId < 12; villageSiteId++) {
        constructionList.add(new ConstructionSite(village.get(), null, villageSiteId));
      }
      constructionSiteRepository.saveAll(constructionList);
    }
  }

  /**
   * Provisions a local account for a Keycloak-authenticated user on first SSO login.
   * No-ops if the username already exists, making it safe to call on every OAuth callback.
   * Falls back to {@code username@keycloak.local} when no email is provided.
   */
  public void provisionKeycloakUser(String username, String email) {
    if (userRepository.findByUsername(username).isPresent()) {
      return;
    }
    String resolvedEmail = (email != null && !email.isBlank()) ? email : username + "@keycloak.local";
    UserInfo userInfo = new UserInfo(UUID.randomUUID(), username, resolvedEmail, UUID.randomUUID().toString(), "ROLE_USER");
    newUser(userInfo);
  }

  /**
   * Returns the profile and village list for the given username.
   *
   * @throws RuntimeException if the username is not found
   */
  public UserDTO getUserInfo(String username) {
    LOG.debug("Getting user info for username {}", username);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Username not found"));

    List<VillageDTO> villageDTOList = user.getVillages().stream().map(Village::toDTO).toList();

    return new UserDTO(user.getId(), username, villageDTOList);
  }

  /**
   * Returns a flat projection of every user paired with their village, used to populate the world map.
   */
    public List<UserVillageFlatDTO> getAllUsersWithVillages() {
        return userRepository.fetchAllUsersWithVillagesFlat();
    }
}
