package knu.dykf.landom.service.analytics;

import knu.dykf.landom.dto.request.analytics.SectionRequest;
import knu.dykf.landom.dto.response.analytics.FunnelResponse;
import knu.dykf.landom.dto.response.analytics.ReplayResponse;
import knu.dykf.landom.dto.response.analytics.SectionSourceResponse;
import knu.dykf.landom.dto.response.analytics.SessionListResponse;
import knu.dykf.landom.dto.response.analytics.SummaryResponse;
import knu.dykf.landom.dto.response.analytics.TrendsResponse;
import knu.dykf.landom.entity.project.FunnelAnalysisStatus;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.event.EventClickHouseRepository;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionRepository;
import knu.dykf.landom.service.project.SectionSourceExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventClickHouseRepository eventClickHouseRepository;
    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final SectionSourceExtractor sectionSourceExtractor;

    @Transactional
    public void saveProjectSections(Long projectId, SectionRequest request) {
        // 1. 해당 프로젝트의 기존 섹션 설정 삭제
        sectionRepository.deleteByProjectId(projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));


        // 2. 새로운 섹션 리스트 생성 및 저장
        List<Section> newSections = request.funnels().stream()
                .map(step -> Section.builder() // Entity에 @Builder가 있다면 사용, 없다면 생성자 사용
                        .project(project)
                        .name(step.name())
                        .cssSelector(step.selector())
                        .cssRules(extractSectionCssRules(project, step.selector()))
                        .stepOrder(step.stepOrder())
                        .build())
                .toList();

        sectionRepository.saveAll(newSections);
        project.completeFunnelAnalysis();
    }

    public FunnelResponse getFunnelAnalytics(String username, Long id) {
        Project project = getProjectAndValidateOwnership(username, id);
        FunnelResponse.Status status = mapStatus(project.getFunnelAnalysisStatus());
        if (status == FunnelResponse.Status.NOT_CREATED) {
            return new FunnelResponse(status, 0, List.of());
        }

        String apiKey = project.getApiKey();
        List<Section> sections = sectionRepository.findByProjectIdOrderByStepOrderAsc(project.getId());
        long totalSessions = eventClickHouseRepository.getTotalSessionCount(apiKey);

        List<FunnelResponse.FunnelData> funnelDataList = new ArrayList<>();
        long previousReachedCount = totalSessions;

        for (int index = 0; index < sections.size(); index++) {
            Section section = sections.get(index);
            List<String> reachedSectionSelectors = sections.subList(index, sections.size()).stream()
                    .map(Section::getCssSelector)
                    .toList();

            Map<String, Object> stats = eventClickHouseRepository.getSectionStats(
                    apiKey, reachedSectionSelectors, section.getCssSelector());

            long reachedCount = getLong(stats, "reached_count");
            double avgDurationSeconds = getDouble(stats, "avg_duration");

            double reachRate = totalSessions == 0 ? 0 : Math.round((double) reachedCount / totalSessions * 100.0) / 100.0;
            double dropRate = previousReachedCount == 0 ? 0 :
                    Math.round((double) (previousReachedCount - reachedCount) / previousReachedCount * 100.0) / 100.0;

            funnelDataList.add(new FunnelResponse.FunnelData(
                    section.getId(),
                    section.getName(),
                    section.getCssSelector(),
                    reachedCount,
                    reachRate,
                    dropRate,
                    formatDuration((long) avgDurationSeconds)
            ));

            previousReachedCount = reachedCount;
        }

        return new FunnelResponse(status, totalSessions, funnelDataList);
    }

    public SectionSourceResponse getSectionSource(String username, Long projectId, Long sectionId) {
        Project project = getProjectAndValidateOwnership(username, projectId);
        Section section = getSectionInProject(projectId, sectionId);

        if (project.getLandingPageHtml() == null || project.getLandingPageHtml().isBlank()) {
            throw new CustomException(ErrorCode.LANDING_PAGE_SNAPSHOT_NOT_FOUND);
        }

        String html = sectionSourceExtractor.extractSectionHtml(
                project.getLandingPageHtml(),
                section.getCssSelector()
        );
        String cssRules = section.getCssRules();
        // 예전 데이터에 부정확한 CSS가 저장돼 있을 수 있어 전체 CSS 스냅샷이 있으면 조회 시점에 다시 추출한다.
        if (project.getLandingPageCss() != null && !project.getLandingPageCss().isBlank()) {
            cssRules = sectionSourceExtractor.extractSectionCssRules(
                    project.getLandingPageHtml(),
                    project.getLandingPageCss(),
                    section.getCssSelector()
            );
        }

        return new SectionSourceResponse(
                section.getId(),
                section.getName(),
                section.getCssSelector(),
                html,
                cssRules == null ? "" : cssRules
        );
    }

    private FunnelResponse.Status mapStatus(FunnelAnalysisStatus status) {
        return switch (status) {
            case IN_PROGRESS -> FunnelResponse.Status.IN_PROGRESS;
            case COMPLETED -> FunnelResponse.Status.COMPLETED;
            case FAILED -> FunnelResponse.Status.FAILED;
            default -> FunnelResponse.Status.NOT_CREATED;
        };
    }

    public SessionListResponse getRecentSessions(
            String username,
            Long id,
            Long sectionId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            int limit
    ) {
        validateSessionQuery(startDate, endDate, limit);

        Project project = getProjectAndValidateOwnership(username, id);
        String apiKey = project.getApiKey();
        List<Section> sections = sectionRepository.findByProjectIdOrderByStepOrderAsc(project.getId());
        String sectionSelector = resolveSectionSelector(project.getId(), sectionId);
        String normalizedStatus = normalizeSessionStatus(status);
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        List<EventClickHouseRepository.SessionSummaryDto> rawSessions =
                eventClickHouseRepository.getRecentSessions(
                        apiKey,
                        sectionSelector,
                        startDateTime,
                        endDateTimeExclusive,
                        normalizedStatus,
                        limit
                );

        List<SessionListResponse.SessionDto> dtoList = rawSessions.stream()
                .map(raw -> mapToDto(raw, sections))
                .collect(Collectors.toList());

        return new SessionListResponse(dtoList);
    }

    public SummaryResponse getAnalyticsSummary(String username, Long id) {

        Project project = getProjectAndValidateOwnership(username, id);
        String apiKey = project.getApiKey();
        List<Section> sections = sectionRepository.findByProjectIdOrderByStepOrderAsc(project.getId());

        // 섹션이 설정되지 않은 경우 기본값 반환
        if (sections.isEmpty()) {
            return new SummaryResponse(0, 0, 0.0, "00:00");
        }

        Map<String, Object> stats = eventClickHouseRepository.getSummaryStats(apiKey);

        long totalSessions = getLong(stats, "total_sessions");
        long convertedSessions = getLong(stats, "converted_sessions");
        double avgDurationSeconds = getDouble(stats, "avg_total_duration");

        double conversionRate = totalSessions == 0 ? 0 :
                Math.round((double) convertedSessions / totalSessions * 1000.0) / 1000.0;

        return new SummaryResponse(
                totalSessions,
                convertedSessions,
                conversionRate,
                formatDuration((long) avgDurationSeconds)
        );
    }

    public TrendsResponse getTrends(String username, Long id) {

        Project project = getProjectAndValidateOwnership(username, id);
        String apiKey = project.getApiKey();
        List<EventClickHouseRepository.TrendRawDto> rawTrends =
                eventClickHouseRepository.getWeeklyTrends(apiKey);

        List<TrendsResponse.TrendUnit<Integer>> scores = new ArrayList<>();
        List<TrendsResponse.TrendUnit<Double>> conversionRates = new ArrayList<>();

        for (var raw : rawTrends) {
            scores.add(new TrendsResponse.TrendUnit<>(raw.period(), raw.score()));

            double rate = raw.totalSessions() == 0 ? 0 :
                    Math.round((double) raw.convertedSessions() / raw.totalSessions() * 1000.0) / 1000.0;
            conversionRates.add(new TrendsResponse.TrendUnit<>(raw.period(), rate));
        }

        return new TrendsResponse(scores, conversionRates);
    }

    public ReplayResponse getSessionReplay(String username, Long id, String sessionId) {
        Project project = getProjectAndValidateOwnership(username, id);
        List<JsonNode> events =
                eventClickHouseRepository.getReplayEvents(project.getApiKey(), sessionId);

        return new ReplayResponse(sessionId, events);
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

    private String extractSectionCssRules(Project project, String selector) {
        // 크롤링 전이거나 CSS 수집에 실패한 경우에도 섹션 저장 자체는 계속 진행한다.
        if (project.getLandingPageHtml() == null
                || project.getLandingPageHtml().isBlank()
                || project.getLandingPageCss() == null
                || project.getLandingPageCss().isBlank()) {
            return "";
        }

        try {
            return sectionSourceExtractor.extractSectionCssRules(
                    project.getLandingPageHtml(),
                    project.getLandingPageCss(),
                    selector
            );
        } catch (CustomException e) {
            return "";
        }
    }

    private void validateSessionQuery(LocalDate startDate, LocalDate endDate, int limit) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (limit <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String resolveSectionSelector(Long projectId, Long sectionId) {
        if (sectionId == null) {
            return null;
        }

        return sectionRepository.findByIdAndProjectId(sectionId, projectId)
                .map(Section::getCssSelector)
                .orElseThrow(() -> new CustomException(ErrorCode.SECTION_NOT_FOUND));
    }

    private String normalizeSessionStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();
        boolean supported = Arrays.asList("CONVERTED", "DROP", "EXPLORING").contains(normalized);
        if (!supported) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return normalized;
    }

    private SessionListResponse.SessionDto mapToDto(
            EventClickHouseRepository.SessionSummaryDto raw,
            List<Section> sections) {

        String device = parseDevice(raw.userAgent());
        String timestamp = raw.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String duration = formatDuration(raw.durationSeconds());

        String lastSectionName = "Unknown";

        if (raw.lastCssSelector() != null && !sections.isEmpty()) {
            for (Section section : sections) {
                if (raw.lastCssSelector().startsWith(section.getCssSelector())) {
                    lastSectionName = section.getName().name();
                }
            }
        }

        String replayUrl = "/replays/" + raw.sessionId();

        return new SessionListResponse.SessionDto(
                raw.sessionId(),
                timestamp,
                device,
                lastSectionName,
                duration,
                raw.status(),
                replayUrl
        );
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String browser = "Unknown";
        String os = "Unknown";

        if (userAgent.contains("Chrome")) browser = "Chrome";
        else if (userAgent.contains("Safari")) browser = "Safari";
        else if (userAgent.contains("Firefox")) browser = "Firefox";

        if (userAgent.contains("Mac OS X")) os = "Mac";
        else if (userAgent.contains("Windows")) os = "Windows";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) os = "iOS";

        return browser + " - " + os;
    }

    private String formatDuration(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private long getLong(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double getDouble(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
