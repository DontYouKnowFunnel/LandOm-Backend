package knu.dykf.landom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.ProjectCreateRequest;
import knu.dykf.landom.dto.request.ProjectUpdateRequest;
import knu.dykf.landom.dto.response.ProjectListResponse;
import knu.dykf.landom.dto.response.ProjectResponse;
import knu.dykf.landom.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Projects", description = "프로젝트 관련 API (내 프로젝트 관리)")
@RestController
@RequestMapping(value = "/api/v1/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@SecurityRequirement(name = "jwtAuth") // Swagger 상단 자물쇠 아이콘 사용
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성하고 API 키를 발급받습니다.")
    @ApiResponse(responseCode = "201", description = "프로젝트 생성 성공")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(userDetails.getUsername(), request));
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "현재 로그인한 사용자가 소유한 프로젝트 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "프로젝트 목록 조회 성공")
    @GetMapping
    public ResponseEntity<ProjectListResponse> getProjectList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectList(userDetails.getUsername()));
    }

    @Operation(summary = "프로젝트 상세 조회", description = "특정 프로젝트의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "프로젝트 상세 조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectDetail(userDetails.getUsername(), id));
    }

    @Operation(summary = "프로젝트 수정", description = "프로젝트의 이름이나 설명을 수정합니다.")
        @ApiResponse(responseCode = "200", description = "프로젝트 수정 성공")
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(projectService.updateProject(userDetails.getUsername(), id, request));
    }

    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 완전히 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "프로젝트 삭제 성공")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        projectService.deleteProject(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "프로젝트 랜딩 페이지 크롤링", description = "프로젝트 ID를 통해 DB에 저장된 URL을 조회한 후 크롤링을 수행합니다.")
    @ApiResponse(responseCode = "200", description = "크롤링 성공")
    @GetMapping("/{id}/crawl")
    public ResponseEntity<String> crawlProjectLandingPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        String html = projectService.crawlProjectHtml(userDetails.getUsername(), id);
        return ResponseEntity.ok(html);
    }
}