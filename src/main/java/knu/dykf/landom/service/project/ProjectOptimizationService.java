package knu.dykf.landom.service.project;

import knu.dykf.landom.dto.request.project.LlmOptimizationRequest;
import knu.dykf.landom.dto.request.project.OptimizationPlanRequest;
import knu.dykf.landom.dto.request.project.OptimizationRequest;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.event.EventClickHouseRepository;
import knu.dykf.landom.repository.event.EventClickHouseRepository.SectionBehaviorData;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Selector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ProjectOptimizationService {

    private final ProjectRepository projectRepository;
    private final SectionRepository sectionRepository;
    private final EventClickHouseRepository eventClickHouseRepository;

    @Value("${llm.server.url}")
    private String llmServerUrl;

    @Transactional(readOnly = true)
    public void requestOptimization(String username, Long projectId, Long sectionId, OptimizationRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getUsername().equals(username)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        Section section = sectionRepository.findByIdAndProjectId(sectionId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));

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

        Section section = sectionRepository.findByIdAndProjectId(sectionId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));

        section.updateOptimizationPlan(request.optimizationPlan());
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
