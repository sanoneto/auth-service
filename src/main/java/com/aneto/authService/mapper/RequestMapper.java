package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RequestMapper {

    // --- Mapeamentos de Utilizador ---

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

    /**
     * Converte a entidade Users e o Token para o LoginResponse (Record).
     */
    @Mapping(target = "message", constant = "message")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "publicId", expression = "java(user.getPublicId() != null ? user.getPublicId().toString() : null)")
    @Mapping(target = "googleToken", source = "user.googleToken")
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().toString() : null)")
    @Mapping(target = "allowedModules", source = "user.allowedModules")
    @Mapping(target = "email", source = "user.email") // ✅ Adicionado mapeamento do email
    @Mapping(target = "numeroSocio", source = "user.numeroSocio")
    LoginResponse mapToUserResponse(Users user, String token , String message);

    List<UsersResponse> mapToUserResponseList(List<Users> users);

    // --- Mapeamentos de Projeto ---

    ProjectResponse mapToProjectResponse(Projects project);
    List<ProjectResponse> mapToProjectResponseList(List<Projects> projects);

    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    Projects mapToProjectEntity(ProjectRequest projectRequest);

    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateProjectFromRequest(ProjectRequest projectRequest, @MappingTarget Projects project);
}