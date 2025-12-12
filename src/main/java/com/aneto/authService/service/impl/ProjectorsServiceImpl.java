package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.Projects;
import com.aneto.authService.models.Users;
import com.aneto.authService.repository.ProjectorsRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.ProjectorsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectorsServiceImpl implements ProjectorsService {

    private final ProjectorsRepository repository;
    private final UsersRepository usersRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<Projects> findAll() {

        return repository.findAll();
    }

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
        return requestMapper.mapTProject(projects);
    }

    @Override
    public List<String> findallbyName(String projectName) {
        return repository.findAll(projectName);
    }


    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);

    }
}
