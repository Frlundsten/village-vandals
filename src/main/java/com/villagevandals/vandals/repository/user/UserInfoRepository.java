package com.villagevandals.vandals.repository.user;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.web.UserInfo;
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
