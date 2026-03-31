package knu.dykf.landom.service;

import knu.dykf.landom.dto.response.FunnelResponse;
import knu.dykf.landom.repository.EventClickHouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventClickHouseRepository eventClickHouseRepository;

    public FunnelResponse getScrollFunnelAnalytics(String projectKey) {
        List<FunnelResponse.FunnelData> data = eventClickHouseRepository.getScrollFunnel(projectKey);

        return new FunnelResponse(data);
    }
}