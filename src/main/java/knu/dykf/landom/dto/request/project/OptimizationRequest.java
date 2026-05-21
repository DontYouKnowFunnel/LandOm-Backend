package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record OptimizationRequest(
        @Schema(description = "방문자 페르소나", example = "20대 대학생, 가격에 민감하고 모바일로 방문")
        @NotBlank
        String persona
) {
}
