package com.aneto.authService.repository;

import com.aneto.authService.models.ProjectId;
import com.aneto.authService.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectsRepository extends JpaRepository<Projects, ProjectId> {

    List<Projects> findByUsername(String username);

    boolean existsByUsernameAndProjectName(String username, String projectName);

    List<Projects> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<Projects> findByUsernameAndProjectName(String username, String projectName);

    void deleteByUsernameIgnoreCase(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Projects p WHERE p.users.id = :userId")
    void deleteByUsersId(@Param("userId") Long userId);
}