package knu.dykf.landom.service.project;

import knu.dykf.landom.dto.llm.LlmCodegenRequest;
import knu.dykf.landom.dto.request.project.CodegenRequest;
import knu.dykf.landom.dto.request.project.CodegenResultRequest;
import knu.dykf.landom.dto.response.project.CodegenResponse;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.RecommendationUsageStatus;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.entity.project.SectionOptimizationRecommendation;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionOptimizationRecommendationRepository;
import knu.dykf.landom.repository.project.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCodegenService {

    private final ProjectRepository projectRepository;
    private final SectionRepository sectionRepository;
    private final SectionOptimizationRecommendationRepository optimizationRecommendationRepository;
    private final SectionSourceExtractor sectionSourceExtractor;

    @Value("${llm.server.url}")
    private String llmServerUrl;

    @Transactional
    public void requestCodegen(String username, Long projectId, Long sectionId, CodegenRequest request) {
        Project project = getProjectAndValidateOwnership(username, projectId);
        Section section = getSectionInProject(projectId, sectionId);

        if (project.getLandingPageHtml() == null
                || project.getLandingPageHtml().isBlank()
                || project.getLandingPageCrawledAt() == null) {
            throw new CustomException(ErrorCode.LANDING_PAGE_SNAPSHOT_NOT_FOUND);
        }

        List<SectionOptimizationRecommendation> recommendations = request.optimizationIds()
                .stream()
                .distinct()
                .map(optimizationId -> getOptimizationInProject(projectId, optimizationId))
                .toList();
        if (recommendations.stream()
                .anyMatch(recommendation -> !recommendation.getSection().getId().equals(section.getId()))) {
            throw new CustomException(ErrorCode.OPTIMIZATION_TARGET_MISMATCH);
        }
        Set<Long> selectedRecommendationIds = recommendations.stream()
                .map(SectionOptimizationRecommendation::getId)
                .collect(Collectors.toSet());
        optimizationRecommendationRepository.findBySectionIdOrderByRankAsc(section.getId())
                .forEach(recommendation -> {
                    if (selectedRecommendationIds.contains(recommendation.getId())) {
                        recommendation.markUsed();
                    } else {
                        recommendation.markUnused();
                    }
                });
        section.markCodeGenerationInProgress();

        String sectionHtml = section.getHtml();
        if (sectionHtml == null || sectionHtml.isBlank()) {
            sectionHtml = sectionSourceExtractor.extractSectionHtml(
                    project.getLandingPageHtml(),
                    section.getCssSelector()
            );
            section.updateSource(sectionHtml, section.getCssRules());
        }
        String sectionCss = section.getCssRules() == null ? "" : section.getCssRules();

        LlmCodegenRequest llmRequest = new LlmCodegenRequest(
                projectId,
                section.getId(),
                sectionHtml,
                sectionCss,
                recommendations.stream()
                        .map(SectionOptimizationRecommendation::toResponse)
                        .toList()
        );

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(llmServerUrl + "/api/v1/funnels/codegen", llmRequest, Void.class);
    }

    @Transactional
    public void updateCodegenResult(Long projectId, Long sectionId, CodegenResultRequest request) {
        Section section = getSectionInProject(projectId, sectionId);
        section.updateGeneratedCode(request.html(), request.css());
    }

    @Transactional(readOnly = true)
    public CodegenResponse getCodegenResult(String username, Long projectId, Long sectionId) {
        getProjectAndValidateOwnership(username, projectId);
        Section section = getSectionInProject(projectId, sectionId);
        List<String> usedRecommendationTitles = optimizationRecommendationRepository
                .findBySectionIdOrderByRankAsc(section.getId())
                .stream()
                .filter(recommendation -> recommendation.getUsageStatus() == RecommendationUsageStatus.USED)
                .map(SectionOptimizationRecommendation::getTitle)
                .toList();

        return new CodegenResponse(
                usedRecommendationTitles,
                section.getCodeGeneratedAt(),
                section.getGeneratedHtml(),
                section.getGeneratedCss()
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

    private Section getSectionInProject(Long projectId, Long sectionId) {
        return sectionRepository.findByIdAndProjectId(sectionId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));
    }
}
