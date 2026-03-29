package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "프로젝트 정보 응답")
public record ProjectResponse(
        @Schema(description = "프로젝트 ID", example = "1")
        Long id,

        @Schema(description = "프로젝트명", example = "마이 프로젝트")
        String name,

        @Schema(description = "프로젝트 설명", example = "프로젝트에 대한 간단한 설명입니다.")
        String description,

        @Schema(description = "API 키", example = "550e8400-e29b-41d4-a716-446655440000")
        String apiKey,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {}
