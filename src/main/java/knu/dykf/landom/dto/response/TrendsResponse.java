package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "주차별 지표 추이 응답")
public record TrendsResponse(
        @Schema(description = "주차별 랜딩 페이지 점수 리스트")
        List<TrendUnit<Integer>> scores,

        @Schema(description = "주차별 전환율 리스트")
        List<TrendUnit<Double>> conversionRates
) {
    public record TrendUnit<T>(
            @Schema(description = "기간 (YYYY-MM-WX)", example = "2026-04-W1")
            String period,
            @Schema(description = "수치")
            T value
    ) {}
}