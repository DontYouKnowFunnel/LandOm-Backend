package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OptimizationPlanRequest(
        @Schema(description = "HTML 개선안 목록")
        @NotEmpty
        List<@Valid OptimizationRecommendationRequest> recommendations
) {
}
