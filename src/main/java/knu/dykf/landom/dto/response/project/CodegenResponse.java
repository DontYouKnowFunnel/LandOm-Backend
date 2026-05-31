package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;

public record CodegenResponse(
        @Schema(description = "개선안이 적용된 HTML", example = "<section>...</section>")
        String generatedHtml,

        @Schema(description = "개선안이 적용된 CSS", example = ".hero { ... }")
        String generatedCss
) {
}
