package com.villagevandals.vandals.user;

import com.villagevandals.vandals.user.dto.UserDTO;
import com.villagevandals.vandals.user.dto.UserRegistrationDTO;
import com.villagevandals.vandals.web.AuthRequest;
import com.villagevandals.vandals.web.jwt.JwtService;
import com.villagevandals.vandals.web.UserInfo;
import java.security.Principal;
import java.util.UUID;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

  private final JwtService jwtService;

  private final AuthenticationManager authenticationManager;

  UserController(
      UserService userService, JwtService jwtService, AuthenticationManager authenticationManager) {
    this.userService = userService;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
  }

  @GetMapping
  public UserDTO currentUserName(Principal principal) {
    LOG.debug("Fetching user info...");
    String username = principal.getName();
    return userService.getUserInfo(username);
  }

    @GetMapping("/validate")
    public void validateAuthentication() {
    }

  @PostMapping("/addNewUser")
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

  @PostMapping("/auth/generateToken")
  public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authRequest.username(), authRequest.password()));
    if (authentication.isAuthenticated()) {
      return jwtService.generateToken(authRequest.username());
    } else {
      throw new UsernameNotFoundException("Invalid user request!");
    }
  }
}
