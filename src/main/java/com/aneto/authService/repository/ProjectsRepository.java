package com.aneto.authService.repository;

import com.aneto.authService.models.ProjectId;
import com.aneto.authService.models.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectsRepository extends JpaRepository<Projects, ProjectId> {

    /**
     * Busca todos os projetos de um usuário específico.
     * Útil para popular a lista de cards no Frontend.
     */
    List<Projects> findByUsername(String username);

    /**
     * Verifica se um projeto existe antes de tentar deletar ou atualizar.
     * Como a chave é composta, o Spring entende que deve buscar por username e projectName.
     */
    boolean existsByUsernameAndProjectName(String username, String projectName);

    List<Projects> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * O JpaRepository já fornece por padrão:
     * - findById(ProjectId id)
     * - deleteById(ProjectId id)
     * - save(Projects entity)
     */
}
