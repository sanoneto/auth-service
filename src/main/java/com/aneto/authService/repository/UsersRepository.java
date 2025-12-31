package com.aneto.authService.repository;


import com.aneto.authService.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmailIgnoreCase(String email);

    Optional<Users> findByUsername(String username);

    @Query("SELECT u FROM Users u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<Users> findByUsernameIgnoreCase(@Param("username") String username);

    Optional<Users> findByEmail(String email);
}

