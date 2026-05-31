package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.entity.project.CodeGenerationStatus;

import java.util.List;

public record OptimizationPlanResponse(
        @Schema(description = "섹션 코드 생성 상태", example = "CODE_NOT_GENERATED")
        CodeGenerationStatus codeGenerationStatus,

        @Schema(description = "HTML 개선안 목록")
        List<OptimizationRecommendationResponse> recommendations
) {
}
