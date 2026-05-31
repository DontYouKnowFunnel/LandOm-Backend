package knu.dykf.landom.service.project;

import knu.dykf.landom.dto.project.OptimizationRecommendation;
import knu.dykf.landom.dto.request.project.CodegenRequest;
import knu.dykf.landom.dto.request.project.CodegenResultRequest;
import knu.dykf.landom.dto.request.project.LlmCodegenRequest;
import knu.dykf.landom.dto.request.project.LlmOptimizationRequest;
import knu.dykf.landom.dto.request.project.OptimizationPlanRequest;
import knu.dykf.landom.dto.request.project.OptimizationRequest;
import knu.dykf.landom.dto.response.project.CodegenResponse;
import knu.dykf.landom.dto.response.project.OptimizationPlanResponse;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.entity.project.SectionOptimizationRecommendation;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.event.EventClickHouseRepository;
import knu.dykf.landom.repository.event.EventClickHouseRepository.SectionBehaviorData;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionOptimizationRecommendationRepository;
import knu.dykf.landom.repository.project.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Selector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectOptimizationService {

    private final ProjectRepository projectRepository;
    private final SectionRepository sectionRepository;
    private final EventClickHouseRepository eventClickHouseRepository;
    private final SectionOptimizationRecommendationRepository optimizationRecommendationRepository;

    @Value("${llm.server.url}")
    private String llmServerUrl;

    @Transactional(readOnly = true)
    public OptimizationPlanResponse getOptimizationPlan(String username, Long projectId, Long sectionId) {
        getProjectAndValidateOwnership(username, projectId);
        Section section = getSectionInProject(projectId, sectionId);

        List<OptimizationRecommendation> recommendations = optimizationRecommendationRepository
                .findBySectionIdOrderByRankAsc(section.getId())
                .stream()
                .map(SectionOptimizationRecommendation::toResponse)
                .toList();

        return new OptimizationPlanResponse(recommendations);
    }

    @Transactional(readOnly = true)
    public void requestOptimization(String username, Long projectId, Long sectionId, OptimizationRequest request) {
        Project project = getProjectAndValidateOwnership(username, projectId);
        Section section = getSectionInProject(projectId, sectionId);

        if (project.getLandingPageHtml() == null
                || project.getLandingPageHtml().isBlank()
                || project.getLandingPageCrawledAt() == null) {
            throw new CustomException(ErrorCode.LANDING_PAGE_SNAPSHOT_NOT_FOUND);
        }

        String sectionHtml = extractSectionHtml(project.getLandingPageHtml(), section.getCssSelector());
        SectionBehaviorData behaviorData = eventClickHouseRepository.getSectionBehaviorData(
                project.getApiKey(),
                section.getCssSelector(),
                project.getLandingPageCrawledAt()
        );

        LlmOptimizationRequest llmRequest = new LlmOptimizationRequest(
                projectId,
                sectionId,
                section.getName(),
                sectionHtml,
                behaviorData,
                request.persona()
        );

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(llmServerUrl + "/api/v1/funnels/optimize", llmRequest, Void.class);
    }

    @Transactional
    public void updateOptimizationPlan(Long projectId, Long sectionId, OptimizationPlanRequest request) {
        if (!projectId.equals(request.projectId()) || !sectionId.equals(request.sectionId())) {
            throw new CustomException(ErrorCode.OPTIMIZATION_TARGET_MISMATCH);
        }

        Section section = getSectionInProject(projectId, sectionId);

        optimizationRecommendationRepository.deleteBySectionId(section.getId());
        List<SectionOptimizationRecommendation> recommendations = request.recommendations()
                .stream()
                .map(recommendation -> new SectionOptimizationRecommendation(section, recommendation))
                .toList();
        optimizationRecommendationRepository.saveAll(recommendations);
    }

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
            String sectionHtml = extractSectionHtml(project.getLandingPageHtml(), section.getCssSelector());
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

    private Section getSectionInProject(Long projectId, Long sectionId) {
        return sectionRepository.findByIdAndProjectId(sectionId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));
    }

    private SectionOptimizationRecommendation getOptimizationInProject(Long projectId, Long optimizationId) {
        return optimizationRecommendationRepository.findByIdAndSection_Project_Id(optimizationId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPTIMIZATION_NOT_FOUND));
    }

    private String extractSectionHtml(String html, String cssSelector) {
        try {
            Element sectionElement = Jsoup.parse(html).selectFirst(cssSelector);
            if (sectionElement == null) {
                throw new CustomException(ErrorCode.SECTION_HTML_NOT_FOUND);
            }

            return sectionElement.outerHtml();
        } catch (Selector.SelectorParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
