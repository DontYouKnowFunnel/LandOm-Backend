package knu.dykf.landom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

@Schema(description = "SDK 이벤트 수집 요청 (통합)")
public record SdkEventRequest(
        @Schema(description = "브라우저 세션 ID", example = "sess_v3_98765")
        @NotBlank(message = "sessionId는 필수입니다.")
        String sessionId,

        @Schema(description = "사용자 에이전트", example = "Mozilla/5.0...")
        String userAgent,

        @Schema(description = "현재 페이지 URL", example = "https://landom.com/landing")
        @NotBlank(message = "url은 필수입니다.")
        String url,

        @Schema(description = "발생 이벤트 목록")
        @Valid
        List<EventDetail> events
) {
    @Schema(description = "개별 이벤트 상세")
    public record EventDetail(
            @Schema(description = "이벤트 타입 (click, scroll, etc.)", example = "click")
            String type,

            @Schema(description = "발생 시각 (ms)", example = "1711884000000")
            long timestamp,

            @Schema(description = "상세 데이터", example = "{\"targetId\": \"btn_cta\"}")
            Map<String, Object> payload
    ) {}
}