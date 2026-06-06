package com.villagevandals.vandals.user;

import com.villagevandals.vandals.constructionsite.ConstructionSite;
import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.user.dto.UserVillageFlatDTO;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageService;
import com.villagevandals.vandals.village.dto.VillageDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
  private final UserRepository userRepository;
  private final VillageService villageService;
  private final ConstructionSiteRepository constructionSiteRepository;

  public UserService(
      UserRepository userRepository,
      VillageService villageService,
      ConstructionSiteRepository constructionSiteRepository) {
    this.userRepository = userRepository;
    this.villageService = villageService;
    this.constructionSiteRepository = constructionSiteRepository;
  }

  /**
   * Creates a new account and starter village for a Keycloak-provisioned user.
   *
   * @throws RuntimeException if the username or email is already taken
   */
  public void newUser(String username, String email, String roles) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("Username already taken");
    }
    if (userRepository.findByEmail(email).isPresent()) {
      throw new RuntimeException("Email already registered");
    }

    var user = new User(UUID.randomUUID(), username, email, roles, new ArrayList<>());

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
    String resolvedEmail =
        (email != null && !email.isBlank()) ? email : username + "@keycloak.local";
    newUser(username, resolvedEmail, "ROLE_USER");
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
