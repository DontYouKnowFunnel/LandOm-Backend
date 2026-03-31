package knu.dykf.landom.controller;

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

    @GetMapping("/analytics/funnel")
    public ResponseEntity<FunnelResponse> getFunnelData(
            @RequestHeader("X-Project-Key") String projectKey) {

        FunnelResponse response = analyticsService.getScrollFunnelAnalytics(projectKey);

        return ResponseEntity.ok(response);
    }
}