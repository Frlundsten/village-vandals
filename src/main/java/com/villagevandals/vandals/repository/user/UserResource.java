package com.villagevandals.vandals.repository.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.repository.village.VillageResource;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class UserResource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String passwordHash;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
  private List<VillageResource> villages;

  public static User toUser(UserResource resource) {
    User user = new User(resource.username, resource.passwordHash, new ArrayList<>());
    List<Village> villages =
        resource.villages.stream()
            .map(villageResource -> villageResource.toVillage(villageResource, user))
            .toList();

    return new User(resource.username, resource.passwordHash, villages);
  }

  public static UserResource toResource(User user) {
    UserResource userResource = new UserResource();
    userResource.username = user.username();
    userResource.passwordHash = user.passwordHash();
    userResource.villages =
        user.villages().stream()
            .map(village -> new VillageResource(village.getX(), village.getY(), userResource))
            .toList();
    return userResource;
  }

  public List<VillageResource> getVillages() {
    return villages;
  }

  public Long getId() {
    return id;
  }

  @Override
  public String toString() {
    return "UserResource{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", passwordHash='" + passwordHash + '\'' +
            ", villages=" + villages +
            '}';
  }
}
