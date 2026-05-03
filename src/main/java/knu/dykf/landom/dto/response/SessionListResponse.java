package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "최근 세션 리스트 응답")
public record SessionListResponse(
        @Schema(description = "세션 목록")
        List<SessionDto> sessions
) {
    @Schema(description = "세션 상세 요약 정보")
    public record SessionDto(
            @Schema(description = "세션 고유 ID", example = "S-19472")
            String sessionId,

            @Schema(description = "세션 시작 시각 (ISO-8601)", example = "2026-05-01T21:35:00Z")
            String timestamp,

            @Schema(description = "접속 기기 및 브라우저 정보", example = "Chrome - Mac")
            String device,

            @Schema(description = "사용자가 마지막으로 머문 섹션 이름", example = "Pricing Section")
            String lastSection,

            @Schema(description = "총 체류 시간 (MM:SS)", example = "02:58")
            String duration,

            @Schema(description = "세션 상태 (CONVERTED: 전환, DROP: 이탈, EXPLORING: 탐색 중)", example = "CONVERTED")
            String status,

            @Schema(description = "세션 재생(Replay)을 위한 URL", example = "/replays/S-19472")
            String replayUrl
    ) {}
}