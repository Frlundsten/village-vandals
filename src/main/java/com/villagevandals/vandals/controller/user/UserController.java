package com.villagevandals.vandals.controller.user;

import com.villagevandals.vandals.service.user.UserService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

  UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public UserDTO currentUserName(Principal principal) {
    String username = principal.getName();
    return userService.getUserInfo(username);
  }
}
