package knu.dykf.landom.controller.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.project.OptimizationPlanRequest;
import knu.dykf.landom.dto.request.project.OptimizationRequest;
import knu.dykf.landom.dto.response.project.OptimizationPlanResponse;
import knu.dykf.landom.service.project.ProjectOptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Optimization", description = "섹션 HTML 개선안 관련 API")
@RestController
@RequestMapping(value = "/api/v1/projects/{projectId}/optimizations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "jwtAuth")
public class OptimizationController {

    private final ProjectOptimizationService projectOptimizationService;

    @Operation(summary = "섹션 HTML 개선안 생성 요청", description = "섹션 HTML, 최신 방문자 행동 데이터, 페르소나를 LLM 서버로 전달합니다.")
    @ApiResponse(responseCode = "204", description = "개선안 생성 요청 성공")
    @PostMapping("/{sectionId}")
    public ResponseEntity<Void> requestSectionOptimization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("projectId") Long projectId,
            @PathVariable Long sectionId,
            @Valid @RequestBody OptimizationRequest request) {

        projectOptimizationService.requestOptimization(userDetails.getUsername(), projectId, sectionId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "섹션 HTML 개선안 조회", description = "섹션별로 저장된 HTML 개선안을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "개선안 조회 성공")
    @GetMapping("/{sectionId}")
    public ResponseEntity<OptimizationPlanResponse> getSectionOptimizationPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("projectId") Long projectId,
            @PathVariable Long sectionId) {

        return ResponseEntity.ok(projectOptimizationService.getOptimizationPlan(
                userDetails.getUsername(), projectId, sectionId));
    }

    @Operation(summary = "섹션 HTML 개선안 저장", description = "LLM 서버가 생성한 섹션별 HTML 개선안을 저장합니다.")
    @ApiResponse(responseCode = "204", description = "개선안 저장 성공")
    @PatchMapping("/{sectionId}")
    public ResponseEntity<Void> updateSectionOptimizationPlan(
            @PathVariable("projectId") Long projectId,
            @PathVariable Long sectionId,
            @Valid @RequestBody OptimizationPlanRequest request) {

        projectOptimizationService.updateOptimizationPlan(projectId, sectionId, request);
        return ResponseEntity.noContent().build();
    }
}
