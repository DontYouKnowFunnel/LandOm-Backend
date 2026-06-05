package knu.dykf.landom.dto.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.entity.project.SectionName;
import knu.dykf.landom.repository.event.EventClickHouseRepository.SectionBehaviorData;

public record LlmOptimizationRequest(
        @Schema(description = "프로젝트 ID", example = "1")
        Long projectId,

        @Schema(description = "섹션 ID", example = "1")
        Long sectionId,

        @Schema(description = "퍼널 섹션 이름", example = "HERO")
        SectionName sectionName,

        @Schema(description = "섹션 HTML", example = "<section>...</section>")
        String sectionHtml,

        @Schema(description = "방문자 행동 데이터")
        SectionBehaviorData visitorBehaviorData,

        @Schema(description = "방문자 페르소나", example = "20대 대학생, 가격에 민감하고 모바일로 방문")
        String persona
) {
}
