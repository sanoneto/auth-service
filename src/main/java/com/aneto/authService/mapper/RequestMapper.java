package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "role", source = "role")
    Users mapToLogin(UserCredentialsRequest userCredentialsRequest);

    @Mapping(target = "projectName", source = "projectName")
    @Mapping(target = "requiredHours", source = "requiredHours")
     ProjectResponse mapTProject(Projects project);


    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "email", source = "email")
    List<UsersResponse> UsersResponse(List<Users> users);

}
