package com.aneto.authService.service;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.models.Projects;

import java.util.List;

public interface ProjectorsService {
    List<Projects> findAll();
    ProjectResponse saveProjets(ProjectRequest projects);
    List<String> findallbyName(String username);
    void deleteById(Long id);
}
