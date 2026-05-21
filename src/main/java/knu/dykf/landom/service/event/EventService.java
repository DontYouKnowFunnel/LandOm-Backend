package knu.dykf.landom.service.event;

import knu.dykf.landom.dto.request.event.SdkEventRequest;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.entity.project.Section;
import knu.dykf.landom.entity.project.SectionName;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.event.EventClickHouseRepository;
import knu.dykf.landom.repository.project.ProjectRepository;
import knu.dykf.landom.repository.project.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final ProjectRepository projectRepository;
    private final SectionRepository sectionRepository;
    private final EventClickHouseRepository eventClickHouseRepository;

    public void collectEvents(String headerKey, SdkEventRequest request) {

        String projectKey = resolveProjectKey(headerKey, request.apiKey());

        if (projectKey == null || projectKey.isBlank()) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }

        Project project = projectRepository.findByApiKey(projectKey)
                .orElseThrow(() -> new CustomException(ErrorCode.HANDLE_ACCESS_DENIED));

        List<Section> sections = sectionRepository.findByProjectIdOrderByStepOrderAsc(project.getId());
        String ctaSectionSelector = findCtaSectionSelector(sections);

        eventClickHouseRepository.saveAll(projectKey, request, ctaSectionSelector);
    }

    private String resolveProjectKey(String headerKey, String bodyKey) {
        if (headerKey != null && !headerKey.isBlank()) {
            return headerKey;
        }
        return bodyKey;
    }

    private String findCtaSectionSelector(List<Section> sections) {
        return sections.stream()
                .filter(section -> section.getName() == SectionName.CTA_SECTION)
                .map(Section::getCssSelector)
                .findFirst()
                .orElse("");
    }
}
