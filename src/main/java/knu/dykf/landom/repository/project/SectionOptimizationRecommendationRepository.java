package knu.dykf.landom.repository.project;

import knu.dykf.landom.entity.project.SectionOptimizationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionOptimizationRecommendationRepository
        extends JpaRepository<SectionOptimizationRecommendation, Long> {

    List<SectionOptimizationRecommendation> findBySectionIdOrderByRankAsc(Long sectionId);

    void deleteBySectionId(Long sectionId);
}
