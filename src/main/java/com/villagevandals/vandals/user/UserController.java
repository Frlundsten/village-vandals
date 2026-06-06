package com.villagevandals.vandals.user;

import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.user.dto.UserVillageFlatDTO;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
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
}
