package knu.dykf.landom.dto.response.project;

import io.swagger.v3.oas.annotations.media.Schema;
import knu.dykf.landom.entity.project.RecommendationUsageStatus;

import java.util.List;

public record OptimizationRecommendationResponse(
        @Schema(description = "개선안 ID", example = "1")
        Long id,

        @Schema(description = "코드 생성 요청에 사용된 개선안 여부", example = "UNUSED")
        RecommendationUsageStatus usageStatus,

        @Schema(description = "개선안 우선순위", example = "1")
        Integer rank,

        @Schema(description = "개선안 제목", example = "CTA의 기대 결과를 명확히 전달")
        String title,

        @Schema(description = "개선안이 적용된 와이어프레임 HTML", example = "<section class=\"py-12\">...</section>")
        String wireframe,

        @Schema(description = "현재 섹션에서 해결할 문제", example = "CTA 문구가 추상적이라 클릭 후 얻는 결과가 명확하지 않습니다.")
        String problem,

        @Schema(description = "변경할 항목 목록")
        List<String> what_to_change,

        @Schema(description = "구현 방향", example = "현재 CTA 버튼 주변에 클릭 후 흐름을 설명하는 보조 문구를 추가합니다.")
        String implementation_direction,

        @Schema(description = "카피 방향 목록")
        List<String> copy_direction,

        @Schema(description = "레이아웃 방향", example = "주요 CTA와 보조 설명을 같은 시각적 그룹 안에 배치합니다.")
        String layout_direction,

        @Schema(description = "기대 행동 변화", example = "방문자가 클릭 후 결과를 더 쉽게 예측해 CTA 탐색 가능성이 높아질 수 있습니다.")
        String expected_behavior_change,

        @Schema(description = "주의점", example = "보조 문구가 길어지면 핵심 CTA가 약해질 수 있습니다.")
        String risk_or_caveat
) {
}
