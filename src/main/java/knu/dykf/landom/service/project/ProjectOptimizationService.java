package knu.dykf.landom.service.project;

import knu.dykf.landom.dto.request.project.OptimizationRequest;
import knu.dykf.landom.entity.project.Project;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectOptimizationService {

    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public void requestOptimization(String username, Long projectId, Long sectionId, OptimizationRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getUser().getUsername().equals(username)) {
            throw new CustomException(ErrorCode.HANDLE_ACCESS_DENIED);
        }
    }
}
