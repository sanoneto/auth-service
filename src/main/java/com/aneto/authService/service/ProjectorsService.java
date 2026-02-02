package com.aneto.authService.service;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;

import java.util.List;

public interface ProjectorsService {
    ProjectResponse saveProjets(ProjectRequest request);
    List<ProjectResponse> findAllByUsername(String username);
    ProjectResponse updateProject(String username, String projectName, ProjectRequest request);
    void deleteProject(String username, String projectName);
}
