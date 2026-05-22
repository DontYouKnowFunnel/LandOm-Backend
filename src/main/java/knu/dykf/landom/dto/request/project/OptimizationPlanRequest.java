package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OptimizationPlanRequest(
        @Schema(description = "프로젝트 ID", example = "1")
        @NotNull
        Long projectId,

        @Schema(description = "섹션 ID", example = "1")
        @NotNull
        Long sectionId,

        @Schema(description = "HTML 개선안", example = "CTA 버튼 문구를 더 구체적으로 변경합니다.")
        @NotBlank
        String optimizationPlan
) {
}
