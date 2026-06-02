package knu.dykf.landom.service.analytics;

import knu.dykf.landom.dto.request.analytics.SectionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FunnelSectionAsyncService {

    private final AnalyticsService analyticsService;
    private final FunnelAnalysisStatusService funnelAnalysisStatusService;

    @Async("funnelAnalysisExecutor")
    public void saveProjectSections(Long projectId, SectionRequest request) {
        try {
            analyticsService.saveProjectSections(projectId, request);
        } catch (Exception e) {
            log.error("funnel section callback processing failed projectId={}", projectId, e);
            funnelAnalysisStatusService.markFailedIfStillInProgress(projectId);
        }
    }
}
