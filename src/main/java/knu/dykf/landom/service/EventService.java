package knu.dykf.landom.service;

import knu.dykf.landom.dto.request.SdkEventRequest;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.EventClickHouseRepository;
import knu.dykf.landom.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final ProjectRepository projectRepository;
    private final EventClickHouseRepository eventClickHouseRepository;

    public void collectEvents(String headerKey, SdkEventRequest request) {

        String projectKey = resolveProjectKey(headerKey, request.apiKey());

        if (projectKey == null || projectKey.isBlank()) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        if (!projectRepository.existsByApiKey(projectKey)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        eventClickHouseRepository.saveAll(projectKey, request);
    }

    private String resolveProjectKey(String headerKey, String bodyKey) {
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey;
        }
        return bodyKey;
    }
}