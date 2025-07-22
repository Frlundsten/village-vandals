package com.villagevandals.vandals.web;

import com.villagevandals.vandals.user.User;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends ListCrudRepository<User, UUID> {
  @Query(
      "SELECT new com.villagevandals.vandals.web.UserInfo(u.id, u.username, u.email, u.password, u.roles) "
          + "FROM User u WHERE u.email = :email")
  Optional<UserInfo> findByEmail(String email);

  @Query(
          "SELECT new com.villagevandals.vandals.web.UserInfo(u.id, u.username, u.email, u.password, u.roles) "
                  + "FROM User u WHERE u.username = :username")
  Optional<UserInfo> findByUsername(String username);
}
