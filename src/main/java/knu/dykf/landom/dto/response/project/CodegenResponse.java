package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record CodegenResponse(
        @Schema(description = "섹션 ID", example = "1")
        Long sectionId,

        @Schema(description = "코드 생성에 사용된 개선안 제목 목록")
        List<String> usedRecommendationTitles,

        @Schema(description = "코드 생성 완료 시각", example = "2026-06-01T12:34:56")
        LocalDateTime generatedAt,

        @Schema(description = "개선안이 적용된 HTML", example = "<section>...</section>")
        String generatedHtml,

        @Schema(description = "개선안이 적용된 CSS", example = ".hero { ... }")
        String generatedCss
) {
}
