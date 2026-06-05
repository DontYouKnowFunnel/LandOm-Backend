package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CodegenResultRequest(
        @Schema(description = "개선안이 적용된 HTML", example = "<section>...</section>")
        @NotBlank
        String html,

        @Schema(description = "개선안이 적용된 CSS", example = ".hero { ... }")
        String css
) {
}
