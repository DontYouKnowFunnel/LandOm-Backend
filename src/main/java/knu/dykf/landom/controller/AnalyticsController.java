package knu.dykf.landom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import knu.dykf.landom.dto.response.FunnelResponse;
import knu.dykf.landom.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "퍼널 데이터 전송", description = "사용자 행동 데이터를 바탕으로 퍼널 이탈률을 전송합니다.")
    @GetMapping("/analytics/funnel")
    public ResponseEntity<FunnelResponse> getFunnelData(
            @Parameter(description = "프로젝트 API 키", required = true)
            @RequestHeader("X-Project-Key") String projectKey) {

        FunnelResponse response = analyticsService.getScrollFunnelAnalytics(projectKey);

        return ResponseEntity.ok(response);
    }
}