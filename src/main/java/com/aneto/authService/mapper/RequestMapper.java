package com.aneto.authService.mapper;

import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "requiredHours", source = "requiredHours")
    Users mapToLogin(UserCredentialsRequest userCredentialsRequest);


}
