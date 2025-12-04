package com.villagevandals.vandals.user;

import java.util.List;
import java.util.Optional;

import com.villagevandals.vandals.user.dto.UserVillageFlatDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
  Optional<User> findByEmail(String email);


    @Query("""
        SELECT u.id AS userId, u.username AS username,
               v.id AS villageId, v.xCoordinate AS x, v.yCoordinate AS y
        FROM User u
        LEFT JOIN u.villages v
    """)
    List<UserVillageFlatDTO> fetchAllUsersWithVillagesFlat();
}
