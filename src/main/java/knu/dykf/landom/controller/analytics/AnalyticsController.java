package knu.dykf.landom.controller.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.analytics.SectionRequest;
import knu.dykf.landom.dto.response.analytics.FunnelResponse;
import knu.dykf.landom.dto.response.analytics.ReplayResponse;
import knu.dykf.landom.dto.response.analytics.SectionSourceResponse;
import knu.dykf.landom.dto.response.analytics.SessionListResponse;
import knu.dykf.landom.dto.response.analytics.SummaryResponse;
import knu.dykf.landom.dto.response.analytics.TrendsResponse;
import knu.dykf.landom.service.analytics.AnalyticsService;
import knu.dykf.landom.service.analytics.FunnelSectionAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Analytics", description = "데이터 분석 관련 API (퍼널 및 세션 조회)")
@RestController
@RequestMapping(value = "/api/v1/projects/{projectId}/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final FunnelSectionAsyncService funnelSectionAsyncService;

    @Operation(summary = "퍼널 분석 데이터 조회", description = "프로젝트의 설정된 섹션별 도달률, 이탈률 및 평균 체류 시간을 조회합니다.")
    @GetMapping("/funnel")
    public ResponseEntity<FunnelResponse> getFunnelAnalytics(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable("projectId") Long projectId) {

        FunnelResponse response = analyticsService.getFunnelAnalytics(userDetails.getUsername(), projectId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "퍼널 섹션 HTML 및 CSS 룰 조회", description = "특정 퍼널 섹션의 HTML과 저장된 CSS 룰을 조회합니다.")
    @GetMapping("/sections/{sectionId}/source")
    public ResponseEntity<SectionSourceResponse> getSectionSource(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable("projectId") Long projectId,
            @Parameter(description = "섹션 ID", example = "1")
            @PathVariable("sectionId") Long sectionId) {

        return ResponseEntity.ok(analyticsService.getSectionSource(
                userDetails.getUsername(),
                projectId,
                sectionId
        ));
    }

    @Operation(summary = "최근 세션 리스트 조회", description = "프로젝트에 접속한 최근 세션들의 상세 정보와 탐색 상태를 조회합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<SessionListResponse> getRecentSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable("projectId") Long projectId,
            @Parameter(description = "마지막으로 머문 섹션 ID", example = "1")
            @RequestParam(required = false) Long sectionId,
            @Parameter(description = "시작일 (yyyy-MM-dd)", example = "2026-05-20")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "종료일 (yyyy-MM-dd)", example = "2026-05-27")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "세션 상태 (CONVERTED/DROP/EXPLORING)", example = "DROP")
            @RequestParam(required = false) String status,
            @Parameter(description = "조회할 세션 개수 (기본값: 4)", example = "10")
            @RequestParam(defaultValue = "4") int limit) {

        SessionListResponse response = analyticsService.getRecentSessions(
                userDetails.getUsername(),
                projectId,
                sectionId,
                startDate,
                endDate,
                status,
                limit
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "세션 리플레이 이벤트 조회", description = "특정 세션의 rrweb 이벤트 배열을 조회합니다.")
    @GetMapping("/sessions/{sessionId}/replay")
    public ResponseEntity<ReplayResponse> getSessionReplay(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID", example = "1")
            @PathVariable("projectId") Long projectId,
            @Parameter(description = "브라우저 세션 ID", example = "sess_v3_98765")
            @PathVariable String sessionId) {

        ReplayResponse response = analyticsService.getSessionReplay(
                userDetails.getUsername(), projectId, sessionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대시보드 상단 요약 정보 조회", description = "세션 수, 전환율, 평균 체류시간 등 핵심 지표 요약을 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getAnalyticsSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID", example = "1") @PathVariable("projectId") Long projectId) {

        return ResponseEntity.ok(analyticsService.getAnalyticsSummary(userDetails.getUsername(), projectId));
    }

    @Operation(summary = "주차별 지표 추이 조회", description = "랜딩 페이지 점수 및 전환율의 주차별 변화 추이를 조회합니다.")
    @GetMapping("/trends")
    public ResponseEntity<TrendsResponse> getTrends(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable("projectId") Long projectId) {

        return ResponseEntity.ok(analyticsService.getTrends(userDetails.getUsername(), projectId));
    }

    @Operation(summary = "퍼널 섹션 설정", description = "랜딩 페이지의 퍼널 분석을 위한 섹션 이름과 선택자를 설정합니다.")
    @PostMapping("/section")
    public ResponseEntity<Void> updateSections(
            @Parameter(description = "프로젝트 ID") @PathVariable("projectId") Long projectId,
            @Valid @RequestBody SectionRequest request) {

        analyticsService.validateProjectExists(projectId);
        funnelSectionAsyncService.saveProjectSections(projectId, request);
        return ResponseEntity.accepted().build();
    }
}
