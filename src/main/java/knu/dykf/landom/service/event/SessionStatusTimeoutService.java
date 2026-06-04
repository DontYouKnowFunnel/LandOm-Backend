package knu.dykf.landom.service.event;

import knu.dykf.landom.repository.event.EventClickHouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "landom.session-status-timeout.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SessionStatusTimeoutService {

    private final EventClickHouseRepository eventClickHouseRepository;

    @Scheduled(fixedDelay = 60_000)
    public void markInactiveExploringSessionsAsDrop() {
        eventClickHouseRepository.markInactiveExploringSessionsAsDrop();
    }
}
