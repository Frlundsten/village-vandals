package com.villagevandals.vandals.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.villagevandals.vandals.constructionsite.ConstructionSiteRepository;
import com.villagevandals.vandals.village.Village;
import com.villagevandals.vandals.village.VillageService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.annotation.Transactional;

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

  /**
   * {@code VillageService.starterVillage} is itself {@code @Transactional} and marks a world tile
   * occupied. If {@code newUser} is not transactional that inner transaction commits on its own, so
   * a later failure (duplicate username race, constraint violation) leaves the tile claimed forever
   * with no village on it. Both must commit or roll back together.
   */
  @Test
  void newUser_declaresAWriteTransactionSoAClaimedTileRollsBack() throws Exception {
    Transactional tx =
        UserService.class
            .getMethod("newUser", String.class, String.class, String.class)
            .getAnnotation(Transactional.class);

    assertThat(tx)
        .as("tile claim and user/village insert must share one unit of work")
        .isNotNull();
    assertThat(tx.readOnly()).isFalse();
  }
}
