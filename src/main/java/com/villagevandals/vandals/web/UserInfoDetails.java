package com.villagevandals.vandals.web;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserInfoDetails implements UserDetails {

  private String username;
  private String password;
  private String email;
  private List<GrantedAuthority> authorities;

  public UserInfoDetails(UserInfo userInfo) {
    this.username = userInfo.username();
    this.email = userInfo.email();
    this.password = userInfo.password();
    this.authorities =
        Stream.of(userInfo.roles().split(","))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

    @Override
  public boolean isEnabled() {
    return true;
  }
}
