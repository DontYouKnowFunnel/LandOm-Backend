package knu.dykf.landom.dto.response.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.entity.project.SectionName;

@Schema(description = "퍼널 섹션 HTML 및 CSS 룰 응답")
public record SectionSourceResponse(
        @Schema(description = "섹션 ID", example = "1")
        Long sectionId,

        @Schema(description = "섹션 이름", example = "HERO")
        SectionName sectionName,

        @Schema(description = "CSS 선택자", example = "section.hero")
        String selector,

        @Schema(description = "섹션 HTML")
        String html,

        @Schema(description = "섹션에 적용되는 CSS 룰")
        String cssRules
) {
}
