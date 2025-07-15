package com.villagevandals.vandals.model.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
public class User implements UserDetails {

  @Id private UUID id;
  private String username;
  private String password;
  private String email;
  private String roles;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private List<Village> villages = new ArrayList<>();

  public User(
      UUID id,
      String username,
      String password,
      String email,
      String roles,
      List<Village> villages) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.email = email;
    this.roles = roles;
    this.villages = villages;
  }

  public User() {}

  public List<Village> getVillages() {
    return villages;
  }

  public UUID getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return "";
  }

  public String getUsername() {
    return username;
  }

  public void addToVillages(Village village) {
    villages.add(village);
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return UserDetails.super.isEnabled();
  }

  @Override
  public String toString() {
    return "UserResource{"
        + "id="
        + id
        + ", username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + ", villages="
        + villages
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(id, user.id)
        && Objects.equals(username, user.username)
        && Objects.equals(password, user.password)
        && Objects.equals(email, user.email)
        && Objects.equals(roles, user.roles)
        && Objects.equals(villages, user.villages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, password, email, roles, villages);
  }
}
