package knu.dykf.landom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import knu.dykf.landom.dto.response.FunnelResponse;
import knu.dykf.landom.dto.response.SessionListResponse;
import knu.dykf.landom.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analytics", description = "데이터 분석 관련 API (퍼널 및 세션 조회)")
@RestController
@RequestMapping("/api/v1/projects/{id}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "퍼널 분석 데이터 조회", description = "프로젝트의 설정된 섹션별 도달률, 이탈률 및 평균 체류 시간을 조회합니다.")
    @GetMapping("/funnel")
    public ResponseEntity<FunnelResponse> getFunnelAnalytics(
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "프로젝트 고유 API 키", example = "39a95663-b6f1-4768-9313-b64f3ad77ab2")
            @RequestHeader("X-Project-Key") String apiKey) {

        FunnelResponse response = analyticsService.getFunnelAnalytics(id, apiKey);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "최근 세션 리스트 조회", description = "프로젝트에 접속한 최근 세션들의 상세 정보와 탐색 상태를 조회합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<SessionListResponse> getRecentSessions(
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "프로젝트 고유 API 키", example = "39a95663-b6f1-4768-9313-b64f3ad77ab2")
            @RequestHeader("X-Project-Key") String apiKey,
            @Parameter(description = "조회할 세션 개수 (기본값: 4)", example = "10")
            @RequestParam(defaultValue = "4") int limit) {

        SessionListResponse response = analyticsService.getRecentSessions(id, apiKey, limit);
        return ResponseEntity.ok(response);
    }
}