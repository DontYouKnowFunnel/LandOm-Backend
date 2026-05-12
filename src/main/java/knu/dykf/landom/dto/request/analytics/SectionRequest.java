package knu.dykf.landom.dto.request.analytics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "퍼널 섹션 설정 요청")
public record SectionRequest(
        @Schema(description = "설정할 퍼널 단계 리스트")
        @Valid
        @NotEmpty(message = "퍼널 단계는 1개 이상이어야 합니다.")
        List<FunnelStep> funnels
) {
    public record FunnelStep(
            @Schema(description = "단계 순서", example = "1")
            @Positive(message = "단계 순서는 1 이상이어야 합니다.")
            int stepOrder,
            @Schema(description = "섹션 이름", example = "Hero Section")
            @NotBlank(message = "섹션 이름은 필수입니다.")
            String name,
            @Schema(description = "CSS 선택자", example = "section.intro")
            @NotBlank(message = "CSS 선택자는 필수입니다.")
            String selector
    ) {}
}
