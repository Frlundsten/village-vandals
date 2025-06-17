package com.villagevandals.vandals.controller.login;

import com.villagevandals.vandals.repository.user.UserRepository;
import com.villagevandals.vandals.repository.user.UserResource;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserResource resource =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    var user = UserResource.toUser(resource);

    return org.springframework.security.core.userdetails.User.withUsername(user.username())
        .password(user.passwordHash())
        .authorities("USER")
        .build();
  }
}
