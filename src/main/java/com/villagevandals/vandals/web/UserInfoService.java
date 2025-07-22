package com.villagevandals.vandals.web;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserInfoService implements UserDetailsService {

  private final UserInfoRepository repository;

  public UserInfoService(UserInfoRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repository
        .findByUsername(username)
        .map(UserInfoDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
