package knu.dykf.landom.dto.response;

import java.util.List;

public record FunnelResponse(List<FunnelData> funnelData) {
    public record FunnelData(int id, double ratio) {}
}
