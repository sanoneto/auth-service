package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RequestMapper {

    Users mapToLogin(UserCredentialsRequest userCredentialsRequest);

    ProjectResponse mapToProjectResponse(Projects project);

    List<UsersResponse> mapToUserResponseList(List<Users> users);

    UsersResponse mapToUserResponse(Users user);
}