package knu.dykf.landom.dto.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LlmRequest(
        @Schema(description = "프로젝트 id", example = "1")
        @NotNull
        Long projectId,

        @Schema(description = "HTML", example = "<head> </head>")
        @NotBlank
        String html
) {
}
