package knu.dykf.landom.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FunnelAnalysisTimeoutService {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final TaskScheduler taskScheduler;
    private final FunnelAnalysisStatusService funnelAnalysisStatusService;

    public void scheduleTimeout(Long projectId) {
        taskScheduler.schedule(
                () -> funnelAnalysisStatusService.markFailedIfStillInProgress(projectId),
                Instant.now().plus(TIMEOUT)
        );
    }
}
