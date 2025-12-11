package com.aneto.authService.controller;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.service.impl.ProjectorsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Marca a classe como um Controller REST
@RequestMapping("/api/v1/projects") // 2. URL base
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectorsServiceImpl projectorsService;


    @Operation(summary = "Retorna o total de horas de um usuário")
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ESTAGIARIO') and #username == authentication.name)")
    public ResponseEntity<ProjectResponse> saveProjets(@RequestBody @Valid ProjectRequest projectRequest) {
        ProjectResponse response = projectorsService.saveProjets(projectRequest);

        return ResponseEntity.ok(response);
    }

    // 3. Mapeamento GET com variável de caminho
    @GetMapping()
    public ResponseEntity<List<String>> getListprojectos(@RequestParam String username) {

        List<String> projectNames = projectorsService.findallbyName(username);
        // 5. Retorno OK (200) com a lista de projetos
        return ResponseEntity.ok(projectNames);
    }
}