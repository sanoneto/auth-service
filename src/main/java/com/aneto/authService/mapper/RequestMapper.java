package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper para conversão entre DTOs e Entidades.
 * unmappedTargetPolicy = ReportingPolicy.IGNORE silencia os avisos de propriedades
 * que existem na Entidade mas não no DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RequestMapper {

    // --- Mapeamentos de Utilizador ---

    /**
     * Mapeia credenciais para a entidade Users.
     * Ignora campos de auditoria e segurança que não vêm do request.
     */
    @Mapping(target = "allowedModules", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "jwtToken", ignore = true)
    @Mapping(target = "googleToken", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "profile_picture_url", ignore = true)
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "facebookId", ignore = true)
    @Mapping(target = "mfaEnabled", ignore = true)
    @Mapping(target = "mfaSecret", ignore = true)
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
     * Converte o Request de criação para a Entidade.
     * Ignora campos automáticos como valores totais e timestamps.
     */
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    Projects mapToProjectEntity(ProjectRequest projectRequest);

    /**
     * Atualiza uma instância existente da entidade com os dados do Request.
     */
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateProjectFromRequest(ProjectRequest projectRequest, @MappingTarget Projects project);
}