package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.exception.ResourceNotFoundException;
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
    @Transactional
    public ProjectResponse saveProjets(ProjectRequest projectRequest) {
        // 1. Validar se o utilizador existe
        Users users = usersRepository.findByUsername(projectRequest.username())
                .orElseThrow(() -> new RuntimeException("Usuário não existe."));

        // 2. Criar e mapear a entidade
        Projects projects = new Projects();
        projects.setUsername(projectRequest.username());
        projects.setProjectName(projectRequest.projectName());
        projects.setRequiredHours(projectRequest.requiredHours());

        // ADICIONADO: Mapear o hourlyRate que vem do DTO
        projects.setHourlyRate(projectRequest.hourlyRate());

        projects.setUsers(users);

        // 3. Salvar na BD
        Projects savedProject = repository.save(projects);

        // 4. Retornar resposta formatada
        return requestMapper.mapToProjectResponse(savedProject);
    }

    @Override
    public List<ProjectResponse> findAllByUsername(String username) {
        List<Projects> projects = repository.findByUsernameOrderByCreatedAtDesc(username);
        return projects.stream()
                .map(requestMapper::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(String username, String oldProjectName, ProjectRequest request) {
        Projects project = repository.findByUsernameAndProjectName(username, oldProjectName)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));

        // Se o nome mudou, removemos o registro com o ID antigo
        if (!oldProjectName.equals(request.projectName())) {
            repository.delete(project);
            // Criamos uma nova instância com os dados do request
            project = requestMapper.mapToProjectEntity(request);
            project.setUsername(username);
        } else {
            requestMapper.updateProjectFromRequest(request, project);
        }

        Projects saved = repository.save(project);
        return requestMapper.mapToProjectResponse(saved);
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

    @Override
    public ProjectResponse findByUsernameAndProjectName(String username, String projectName) {
        return repository.findByUsernameAndProjectName(username, projectName)
                // CORREÇÃO: Usar o mapper injetado para converter Projects -> ProjectResponse
                .map(requestMapper::mapToProjectResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto '" + projectName + "' não encontrado para o utilizador " + username));
    }
}