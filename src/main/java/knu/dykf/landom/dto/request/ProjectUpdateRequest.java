package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로젝트 수정 요청")
public record ProjectUpdateRequest(
        @Schema(description = "변경할 프로젝트명", example = "수정된 프로젝트명")
        @NotBlank(message = "프로젝트명은 필수입니다.")
        String name,

        @Schema(description = "변경할 프로젝트 설명", example = "수정된 프로젝트 설명입니다.")
        String description
) {}