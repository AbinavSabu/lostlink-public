
package com.yourteam.lostfound.repository;
import com.yourteam.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Retrieve user by email for authentication/loginS
    Optional<User> findByEmail(String email);

    // Fast check to prevent duplicate registrations
    boolean existsByEmail(String email);
}