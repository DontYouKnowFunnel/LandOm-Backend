package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.dto.project.OptimizationRecommendation;

public record LlmCodegenRequest(
        @Schema(description = "프로젝트 ID", example = "1")
        Long projectId,

        @Schema(description = "섹션 ID", example = "1")
        Long sectionId,

        @Schema(description = "개선안 ID", example = "1")
        Long optimizationId,

        @Schema(description = "섹션 HTML", example = "<section>...</section>")
        String sectionHtml,

        @Schema(description = "섹션 CSS", example = ".hero { ... }")
        String sectionCss,

        @Schema(description = "코드 생성에 사용할 개선안 JSON")
        OptimizationRecommendation optimizationPlan
) {
}
