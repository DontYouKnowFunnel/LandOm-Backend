package knu.dykf.landom.repository.project;

import knu.dykf.landom.entity.project.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByProjectIdOrderByStepOrderAsc(Long projectId);
    void deleteByProjectId(Long projectId);
}