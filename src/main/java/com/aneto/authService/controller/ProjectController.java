package com.aneto.authService.controller;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.service.ProjectorsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectorsService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasAnyRole('USER', 'ESTAGIARIO') and #projectRequest.username() == authentication.name)")
    public ResponseEntity<ProjectResponse> saveProjeto(@RequestBody @Valid ProjectRequest projectRequest) {
        ProjectResponse response = projectService.saveProjets(projectRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public ResponseEntity<List<ProjectResponse>> getListProjetos(@RequestParam String username) {
        List<ProjectResponse> projects = projectService.findAllByUsername(username);
        return ResponseEntity.ok(projects);
    }

    @Operation(summary = "Atualiza um projeto existente")
    @PutMapping("/{username}/{projectName}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String username,
            @PathVariable String projectName,
            @RequestBody @Valid ProjectRequest projectRequest) {

        ProjectResponse response = projectService.updateProject(username, projectName, projectRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove um projeto")
    @DeleteMapping("/{username}/{projectName}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String username,
            @PathVariable String projectName) {

        projectService.deleteProject(username, projectName);
        return ResponseEntity.noContent().build();
    }
}