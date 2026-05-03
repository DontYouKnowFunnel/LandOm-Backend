package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "SDK 이벤트 수집 요청 (CSS Selector 기반)")
public record SdkEventRequest(
        @Schema(description = "브라우저 세션 ID", example = "sess_v3_98765")
        @NotBlank(message = "sessionId는 필수입니다.")
        String sessionId,

        @Schema(description = "사용자 에이전트 (브라우저/기기 정보)", example = "Mozilla/5.0...")
        String userAgent,

        @Schema(description = "현재 페이지 URL", example = "https://landom.com/landing")
        @NotBlank(message = "url은 필수입니다.")
        String url,

        @Schema(description = "API 키 (sendBeacon 통신 등 헤더 누락 시 대체용)", example = "proj_12345")
        String apiKey,

        @Schema(description = "발생 이벤트 목록")
        @Valid
        @NotNull
        List<EventDetail> events
) {
    @Schema(description = "개별 이벤트 상세")
    public record EventDetail(
            @Schema(description = "이벤트 타입 (start, click, scroll, visibility, exit, input)", example = "click")
            @NotBlank(message = "이벤트 타입은 필수입니다.")
            String type,

            @Schema(description = "발생 시각 (Epoch Millis)", example = "1711884000000")
            long timestamp,

            @Schema(description = "이벤트 발생 요소의 고유 CSS Selector", example = "main > section.pricing > div.card > button")
            String cssSelector,

            @Schema(description = "타입별 개별 데이터 (예: scroll의 경우 yOffset 등)", example = "{\"isVisible\": true}")
            Map<String, Object> payload
    ) {}
}