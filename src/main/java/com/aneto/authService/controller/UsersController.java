package com.aneto.authService.controller;

import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsersController {

    private final AuthServiceImpl authServiceImpl;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/all")
    public ResponseEntity<List<UsersResponse>> getListUsers() {

        List<UsersResponse> usersResponses = authServiceImpl.findAll();
        // 5. Retorno OK (200) com a lista de projetos
        return ResponseEntity.ok(usersResponses);
    }

}
