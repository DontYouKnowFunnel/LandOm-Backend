package knu.dykf.landom.dto.response;

import java.util.List;

public record SessionListResponse(List<SessionDto> sessions) {
    public record SessionDto(
            String sessionId,
            String timestamp,
            String device,
            String lastSection,
            String duration,
            String status,
            String replayUrl
    ) {}
}
