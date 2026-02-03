package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    // --- Mapeamentos de Utilizador ---
    Users mapToLogin(UserCredentialsRequest userCredentialsRequest);

    UsersResponse mapToUserResponse(Users user);

    List<UsersResponse> mapToUserResponseList(List<Users> users);

    // --- Mapeamentos de Projeto ---

    /**
     * Converte a Entidade JPA para o DTO de Resposta (usado no GET e após UPDATE)
     */
    ProjectResponse mapToProjectResponse(Projects project);

    /**
     * Converte a lista de entidades para lista de DTOs
     */
    List<ProjectResponse> mapToProjectResponseList(List<Projects> projects);

    /**
     * Converte o Request de criação para a Entidade
     */
    Projects mapToProjectEntity(ProjectRequest projectRequest);

    /**
     * Atualiza uma instância existente da entidade com os dados do Request
     * (Muito útil para o método UPDATE no Service)
     */
    void updateProjectFromRequest(ProjectRequest projectRequest, @MappingTarget Projects project);
}