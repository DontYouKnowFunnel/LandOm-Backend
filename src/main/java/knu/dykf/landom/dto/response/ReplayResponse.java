package knu.dykf.landom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Schema(description = "rrweb 세션 리플레이 이벤트 응답")
public record ReplayResponse(
        @Schema(description = "브라우저 세션 ID", example = "sess_v3_98765")
        String sessionId,

        @Schema(description = "rrweb eventWithTime 배열")
        List<JsonNode> events
) {}
