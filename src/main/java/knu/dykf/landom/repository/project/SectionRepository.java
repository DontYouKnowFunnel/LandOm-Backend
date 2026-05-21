package knu.dykf.landom.repository.project;

import knu.dykf.landom.entity.project.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByProjectIdOrderByStepOrderAsc(Long projectId);
    Optional<Section> findByIdAndProjectId(Long id, Long projectId);
    void deleteByProjectId(Long projectId);
}
