package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "퍼널 섹션 설정 요청")
public record SectionRequest(
        @Schema(description = "설정할 퍼널 단계 리스트")
        List<FunnelStep> funnels
) {
    public record FunnelStep(
            @Schema(description = "단계 순서", example = "1")
            int stepOrder,
            @Schema(description = "섹션 이름", example = "Hero Section")
            String name,
            @Schema(description = "CSS 선택자", example = "section.intro")
            String selector
    ) {}
}