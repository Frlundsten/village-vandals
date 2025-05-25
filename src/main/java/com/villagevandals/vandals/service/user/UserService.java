package com.villagevandals.vandals.service.user;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.repository.user.UserRepository;
import com.villagevandals.vandals.repository.user.UserResource;
import java.util.ArrayList;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  UserRepository userRepository;
  PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User newUser(String username, String rawPassword) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new RuntimeException("Username already taken");
    }

    return new User(username, passwordEncoder.encode(rawPassword), new ArrayList<>());
  }

  public void saveNewUser(User user) {
    userRepository.saveAndFlush(UserResource.toResource(user));
  }
}
