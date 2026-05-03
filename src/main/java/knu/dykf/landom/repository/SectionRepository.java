package knu.dykf.landom.repository;

import knu.dykf.landom.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByProjectIdOrderByStepOrderAsc(Long projectId);
    void deleteByProjectId(Long projectId);
}