package com.villagevandals.vandals.repository.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserResource, Long> {
  Optional<UserResource> findByUsername(String username);
}
