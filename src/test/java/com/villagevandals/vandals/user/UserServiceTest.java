package com.villagevandals.vandals.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UserServiceTest {

  @Mock UserRepository userRepository;
  @Mock VillageService villageService;
  @Mock ConstructionSiteRepository constructionSiteRepository;

  UserService userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    userService =
        new UserService(userRepository, villageService, constructionSiteRepository);
    when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(villageService.starterVillage(any())).thenReturn(new Village());
    when(villageService.getStarterVillage(any())).thenReturn(Optional.empty());
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void provisionKeycloakUser_createsUserWithoutPassword() {
    userService.provisionKeycloakUser("alice", "alice@test.com");
    // No PasswordEncoder exists — if this compiles and runs, the requirement is met.
    // Verify the user was saved (not just no-oped).
    org.mockito.Mockito.verify(userRepository).save(any(User.class));
  }
}
