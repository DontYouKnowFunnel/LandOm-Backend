package knu.dykf.landom.service;

import knu.dykf.landom.dto.response.FunnelResponse;
import knu.dykf.landom.dto.response.SessionListResponse;
import knu.dykf.landom.entity.Section;
import knu.dykf.landom.repository.EventClickHouseRepository;
import knu.dykf.landom.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventClickHouseRepository eventClickHouseRepository;
    private final SectionRepository sectionRepository;

    public FunnelResponse getScrollFunnelAnalytics(String projectKey) {
        List<FunnelResponse.FunnelData> data = null;

        return new FunnelResponse(data);
    }

    public SessionListResponse getRecentSessions(Long id, String apiKey, int limit) {

        List<Section> sections = sectionRepository.findByProjectIdOrderByStepOrderAsc(id);
        List<EventClickHouseRepository.SessionSummaryDto> rawSessions =
                eventClickHouseRepository.getRecentSessions(apiKey, limit);

        List<SessionListResponse.SessionDto> dtoList = rawSessions.stream()
                .map(raw -> mapToDto(raw, sections))
                .collect(Collectors.toList());

        return new SessionListResponse(dtoList);
    }

    private SessionListResponse.SessionDto mapToDto(
            EventClickHouseRepository.SessionSummaryDto raw,
            List<Section> sections) {

        String device = parseDevice(raw.userAgent());
        String timestamp = raw.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String duration = formatDuration(raw.durationSeconds());

        String lastSectionName = "Unknown";
        String status = "DROP";
        boolean isConverted = false;

        if (raw.lastCssSelector() != null && !sections.isEmpty()) {
            for (Section section : sections) {
                if (raw.lastCssSelector().startsWith(section.getCssSelector())) {
                    lastSectionName = section.getName();
                    if (section.getStepOrder() == sections.size()) {
                        isConverted = true;
                    }
                }
            }
        }

        if (isConverted) {
            status = "CONVERTED";
        } else if (raw.endTime().isAfter(LocalDateTime.now().minusMinutes(10))) {
            status = "EXPLORING";
        }

        String replayUrl = "/replays/" + raw.sessionId();

        return new SessionListResponse.SessionDto(
                raw.sessionId(),
                timestamp,
                device,
                lastSectionName,
                duration,
                status,
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
}