package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로젝트 목록 응답")
public record ProjectListResponse(
        @Schema(description = "프로젝트 목록")
        List<ProjectResponse> projects
) {}