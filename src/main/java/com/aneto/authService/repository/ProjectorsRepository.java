package com.aneto.authService.repository;

import com.aneto.authService.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectorsRepository extends JpaRepository<Projects, Long> {
    Projects findByProjectName(String projectName);

    @Query("SELECT p FROM Projects p WHERE p.username = :username")
     List<Projects> findallbyName(String username);
}
