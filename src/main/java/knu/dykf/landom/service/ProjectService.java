package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.LlmRequest;
import knu.dykf.landom.dto.request.ProjectCreateRequest;
import knu.dykf.landom.dto.request.ProjectUpdateRequest;
import knu.dykf.landom.dto.response.ProjectListResponse;
import knu.dykf.landom.dto.response.ProjectResponse;
import knu.dykf.landom.entity.Project;
import knu.dykf.landom.entity.User;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.ProjectRepository;
import knu.dykf.landom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final CrawlingService crawlingService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Value("${llm.server.url}")
    private String llmServerUrl;

    @Transactional
    public ProjectResponse createProject(String username, ProjectCreateRequest request) {
        User user = getUserOrThrow(username);

        Project project = Project.builder()
                .user(user)
                .name(request.name())
                .description(request.description())
                .url(request.url())
                .build();

        Project savedProject = projectRepository.save(project);

        return mapToResponse(savedProject);
    }

    public ProjectListResponse getProjectList(String username) {
        User user = getUserOrThrow(username);
        List<Project> projects = projectRepository.findAllByUserId(user.getId());

        List<ProjectResponse> projectResponses = projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new ProjectListResponse(projectResponses);
    }

    public ProjectResponse getProjectDetail(String username, Long projectId) {
        Project project = getProjectAndValidateOwnership(username, projectId);
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(String username, Long projectId, ProjectUpdateRequest request) {
        Project project = getProjectAndValidateOwnership(username, projectId);

        project.updateInfo(request.name(), request.description(), request.url());

        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(String username, Long projectId) {
        Project project = getProjectAndValidateOwnership(username, projectId);
        projectRepository.delete(project);
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Project getProjectAndValidateOwnership(String username, Long projectId) {
        User user = getUserOrThrow(username);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().equals(user)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        return project;
    }

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getUrl(),
                project.getApiKey(),
                project.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public void crawlProjectHtml(String username, Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getUsername().equals(username)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        String targetUrl = project.getUrl();
        String html = crawlingService.crawlLandingPage(targetUrl);
        String funnelAnalyzeUrl = llmServerUrl + "/api/v1/funnels/analyze";

        RestTemplate restTemplate = new RestTemplate();
        LlmRequest request = new LlmRequest(projectId, html);

        try {
            restTemplate.postForEntity(funnelAnalyzeUrl, request, Void.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.LLM_SERVER_ERROR);
        }
    }
}