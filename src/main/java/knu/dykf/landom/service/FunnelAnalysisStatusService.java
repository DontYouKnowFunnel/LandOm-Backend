package knu.dykf.landom.service;

import knu.dykf.landom.entity.Project;
import knu.dykf.landom.entity.FunnelAnalysisStatus;
import knu.dykf.landom.exception.CustomException;
import knu.dykf.landom.exception.ErrorCode;
import knu.dykf.landom.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FunnelAnalysisStatusService {

    private final ProjectRepository projectRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInProgress(Long projectId) {
        Project project = getProject(projectId);
        project.startFunnelAnalysis();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNotCreated(Long projectId) {
        Project project = getProject(projectId);
        project.resetFunnelAnalysis();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedIfStillInProgress(Long projectId) {
        Project project = getProject(projectId);
        if (project.getFunnelAnalysisStatus() == FunnelAnalysisStatus.IN_PROGRESS) {
            project.failFunnelAnalysis();
        }
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
