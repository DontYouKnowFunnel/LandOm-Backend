package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "퍼널 분석 데이터 응답")
public record FunnelResponse(
        @Schema(description = "분석 기간 내 총 세션 수", example = "10248")
        long totalSessions,

        @Schema(description = "섹션별 퍼널 상세 데이터")
        List<FunnelData> funnelData
) {
    @Schema(description = "섹션별 도달 및 이탈 통계")
    public record FunnelData(
            @Schema(description = "섹션 이름", example = "Hero Section")
            String sectionName,

            @Schema(description = "해당 섹션에 도달한 고유 세션 수", example = "10248")
            long reachedUserCount,

            @Schema(description = "전체 세션 대비 도달률 (0.0 ~ 1.0)", example = "1.0")
            double reachRate,

            @Schema(description = "이전 단계 대비 이탈률 (0.0 ~ 1.0)", example = "0.27")
            double dropRate,

            @Schema(description = "해당 섹션 내 평균 체류 시간 (MM:SS)", example = "00:48")
            String avgDuration
    ) {}
}