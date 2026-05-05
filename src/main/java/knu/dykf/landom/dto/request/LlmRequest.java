package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LlmRequest(
        @Schema(description = "프로젝트 id", example = "1")
        @NotBlank
        Long projectId,

        @Schema(description = "HTML", example = "<head> </head>")
        @NotBlank
        String html
) {
}
