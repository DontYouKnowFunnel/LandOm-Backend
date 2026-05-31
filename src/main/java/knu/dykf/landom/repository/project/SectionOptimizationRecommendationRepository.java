package knu.dykf.landom.repository.project;

import knu.dykf.landom.entity.project.SectionOptimizationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionOptimizationRecommendationRepository
        extends JpaRepository<SectionOptimizationRecommendation, Long> {

    List<SectionOptimizationRecommendation> findBySectionIdOrderByRankAsc(Long sectionId);

    Optional<SectionOptimizationRecommendation> findByIdAndSection_Project_Id(Long id, Long projectId);

    void deleteBySectionId(Long sectionId);

    void deleteBySection_Project_Id(Long projectId);
}
