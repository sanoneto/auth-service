package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.ProjectId;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import com.aneto.authService.repository.ProjectsRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.ProjectorsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectorsServiceImpl implements ProjectorsService {

    private final ProjectsRepository repository;
    private final UsersRepository usersRepository;
    private final RequestMapper requestMapper;

    @Override
    public ProjectResponse saveProjets(ProjectRequest projectRequest) {
        Users users = usersRepository.findByUsername(projectRequest.username())
                .orElseThrow(() -> new RuntimeException("user não existe."));
        Projects projects = new Projects();
        projects.setUsername(projectRequest.username());
        projects.setProjectName(projectRequest.projectName());
        projects.setRequiredHours(projectRequest.requiredHours());
        projects.setUsers(users);
        repository.save(projects);
        return requestMapper.mapToProjectResponse(projects);
    }


    @Override
    public List<ProjectResponse> findAllByUsername(String username) {
        // 1. Busca a lista de entidades
        List<Projects> projects = repository.findByUsernameOrderByCreatedAtDesc(username);

        // 2. Converte para a lista de DTOs (ProjectResponse) usando o Mapper
        return projects.stream()
                .map(requestMapper::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(String username, String projectName, ProjectRequest request) {
        // 1. Criamos a chave composta
        ProjectId id = new ProjectId(username, projectName);

        // 2. Buscamos o projeto existente
        Projects project = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        // 3. Atualizamos os campos (exceto os que formam a PK se não quiser mudar a identidade)
        project.setRequiredHours(request.requiredHours());
        project.setHourlyRate(request.hourlyRate());

        Projects updated = repository.save(project);
        return requestMapper.mapToProjectResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProject(String username, String projectName) {
        ProjectId id = new ProjectId(username, projectName);

        if (!repository.existsById(id)) {
            throw new RuntimeException("Projeto não encontrado para exclusão");
        }

        repository.deleteById(id);
    }
}
