package com.aneto.authService.repository;

import com.aneto.authService.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectsRepository extends JpaRepository<Projects, Long> {

    @Query(value = "SELECT " +
            "   distinct( project_name)" +
            "FROM " +
            "    auth.tb_projetos p " +
            "WHERE " +
            "    1=1 " +
            "   AND (:username = 'all' OR p.username = :username)",
            nativeQuery = true)
     List<String> findAll  (String username);

    void deleteByUsersId(Long userId);
}
