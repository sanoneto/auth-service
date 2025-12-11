package com.aneto.authService.repository;

import com.aneto.authService.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectorsRepository extends JpaRepository<Projects, Long> {
    Projects findByProjectName(String projectName);

    @Query(value = "SELECT " +
            "   distinct( project_name)" +
            "FROM " +
            "    auth.tb_projetos p " +
            "WHERE " +
            "    1=1 " +
            "   AND (:project_param = 'all' OR p.project_name = :project_param)",
            nativeQuery = true)
     List<String> findallbyName(String project_param);
}
