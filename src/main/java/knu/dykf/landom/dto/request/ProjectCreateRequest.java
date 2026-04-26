package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(
        @Schema(description = "프로젝트명", example = "마이 프로젝트")
        @NotBlank(message = "프로젝트명은 필수입니다.")
        String name,

        @Schema(description = "프로젝트 설명", example = "프로젝트에 대한 간단한 설명입니다.")
        String description,

        @Schema(description = "랜딩 페이지 url", example = "https://www.google.com")
        String url
) {}