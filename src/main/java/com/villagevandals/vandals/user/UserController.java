package com.villagevandals.vandals.user;

import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.user.dto.UserRegistrationDTO;
import com.villagevandals.vandals.user.dto.UserVillageFlatDTO;
import com.villagevandals.vandals.web.UserInfo;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
  private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * GET /user — returns the profile and village list for the currently authenticated user.
   */
  @GetMapping
  public UserDTO currentUserName(Principal principal) {
    LOG.debug("Fetching user info...");
    String username = principal.getName();
    return userService.getUserInfo(username);
  }

  /**
   * GET /user/all — returns all users with their villages, used to populate the world map.
   */
  @GetMapping("/all")
  public List<UserVillageFlatDTO> allUsers() {
    return userService.getAllUsersWithVillages();
  }

  /**
   * POST /user/register — creates a new account. Returns 400 if the username or email is already taken.
   */
  @PostMapping("/register")
  public ResponseEntity<String> addNewUser(@Valid @RequestBody UserRegistrationDTO dto) {
    UserInfo userInfo =
        new UserInfo(UUID.randomUUID(), dto.username(), dto.email(), dto.password(), "ROLE_USER");
    try {
      userService.newUser(userInfo);
      return ResponseEntity.ok("User created successfully");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
