package com.villagevandals.vandals.service.user;

import com.villagevandals.vandals.controller.user.UserDTO;
import com.villagevandals.vandals.controller.village.VillageDTO;
import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.user.UserRepository;
import com.villagevandals.vandals.repository.user.UserResource;
import com.villagevandals.vandals.repository.village.VillageResource;
import com.villagevandals.vandals.service.village.VillageService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      VillageService villageService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.villageService = villageService;
  }

  public User newUser(String username, String rawPassword) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("Username already taken");
    }

    return new User(username, passwordEncoder.encode(rawPassword), new ArrayList<>());
  }

  public UserResource saveNewUser(User user) {
    return userRepository.save(UserResource.toResource(user));
  }

  public UserDTO getUserInfo(String username) {
    LOG.debug("Getting user info for username {}", username);

    UserResource user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Username not found"));

    List<VillageDTO> villageDTOList = user.getVillages().stream().map(VillageResource::toDTO).toList();

    return new UserDTO(user.getId(), username, villageDTOList);
  }
}
