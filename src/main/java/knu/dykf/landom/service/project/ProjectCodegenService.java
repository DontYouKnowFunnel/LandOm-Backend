package knu.dykf.landom.service.project;

import knu.dykf.landom.dto.llm.LlmCodegenRequest;
import knu.dykf.landom.dto.request.project.CodegenRequest;
import knu.dykf.landom.dto.request.project.CodegenResultRequest;
import knu.dykf.landom.dto.response.project.CodegenResponse;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.entity.project.SectionOptimizationRecommendation;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionOptimizationRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ProjectCodegenService {

    private final ProjectRepository projectRepository;
    private final SectionOptimizationRecommendationRepository optimizationRecommendationRepository;
    private final SectionSourceExtractor sectionSourceExtractor;

    @Value("${llm.server.url}")
    private String llmServerUrl;

    @Transactional(readOnly = true)
    public void requestCodegen(String username, Long projectId, CodegenRequest request) {
        Project project = getProjectAndValidateOwnership(username, projectId);

        if (project.getLandingPageHtml() == null
                || project.getLandingPageHtml().isBlank()
                || project.getLandingPageCrawledAt() == null) {
            throw new CustomException(ErrorCode.LANDING_PAGE_SNAPSHOT_NOT_FOUND);
        }

        RestTemplate restTemplate = new RestTemplate();
        for (Long optimizationId : request.optimizationIds().stream().distinct().toList()) {
            SectionOptimizationRecommendation recommendation = getOptimizationInProject(projectId, optimizationId);
            Section section = recommendation.getSection();
            String sectionHtml = sectionSourceExtractor.extractSectionHtml(
                    project.getLandingPageHtml(),
                    section.getCssSelector()
            );
            String sectionCss = section.getCssRules() == null ? "" : section.getCssRules();

            LlmCodegenRequest llmRequest = new LlmCodegenRequest(
                    projectId,
                    section.getId(),
                    recommendation.getId(),
                    sectionHtml,
                    sectionCss,
                    recommendation.toResponse()
            );

            restTemplate.postForEntity(llmServerUrl + "/api/v1/funnels/codegen", llmRequest, Void.class);
        }
    }

    @Transactional
    public void updateCodegenResult(Long projectId, Long optimizationId, CodegenResultRequest request) {
        SectionOptimizationRecommendation recommendation = getOptimizationInProject(projectId, optimizationId);
        recommendation.updateGeneratedCode(request.html(), request.css());
    }

    @Transactional(readOnly = true)
    public CodegenResponse getCodegenResult(String username, Long projectId, Long optimizationId) {
        getProjectAndValidateOwnership(username, projectId);
        SectionOptimizationRecommendation recommendation = getOptimizationInProject(projectId, optimizationId);

        return new CodegenResponse(
                recommendation.getGeneratedHtml(),
                recommendation.getGeneratedCss()
        );
    }

    private Project getProjectAndValidateOwnership(String username, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getUsername().equals(username)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        return project;
    }

    private SectionOptimizationRecommendation getOptimizationInProject(Long projectId, Long optimizationId) {
        return optimizationRecommendationRepository.findByIdAndSection_Project_Id(optimizationId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTIMIZATION_NOT_FOUND));
    }
}
