package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;

public record OptimizationPlanResponse(
        @Schema(description = "HTML 개선안", example = "CTA 버튼 문구를 더 구체적으로 변경합니다.")
        String optimizationPlan
) {
}
