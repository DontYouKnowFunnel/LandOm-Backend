package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import knu.dykf.landom.dto.project.OptimizationRecommendation;

import java.util.List;

public record OptimizationPlanRequest(
        @Schema(description = "프로젝트 ID", example = "1")
        @NotNull
        Long projectId,

        @Schema(description = "섹션 ID", example = "1")
        @NotNull
        Long sectionId,

        @Schema(description = "HTML 개선안 목록")
        @NotEmpty
        List<@Valid OptimizationRecommendation> recommendations
) {
}
