package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대시보드 상단 요약 정보 응답")
public record SummaryResponse(
        @Schema(description = "전체 세션 수", example = "58912")
        long sessionCount,

        @Schema(description = "전체 전환율 (0.0 ~ 1.0)", example = "0.482")
        double conversionRate,

        @Schema(description = "전체 평균 체류 시간 (MM:SS)", example = "03:24")
        String avgDuration
) {}