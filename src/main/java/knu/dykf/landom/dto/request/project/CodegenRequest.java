package knu.dykf.landom.dto.request.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CodegenRequest(
        @Schema(description = "코드 생성을 요청할 개선안 ID 목록", example = "[1, 2, 3]")
        @NotEmpty
        List<@NotNull Long> optimizationIds
) {
}
