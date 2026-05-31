package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.dto.project.OptimizationRecommendation;

import java.util.List;

public record OptimizationPlanResponse(
        @Schema(description = "HTML 개선안 목록")
        List<OptimizationRecommendation> recommendations
) {
}
