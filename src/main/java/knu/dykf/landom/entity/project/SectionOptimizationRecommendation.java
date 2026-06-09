package knu.dykf.landom.entity.project;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import knu.dykf.landom.dto.request.project.OptimizationRecommendationRequest;
import knu.dykf.landom.dto.response.project.OptimizationRecommendationResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "optimization_recommendations",
        indexes = {
                @Index(
                        name = "idx_opt_recs_section_rank",
                        columnList = "section_id, recommendation_rank"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectionOptimizationRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "recommendation_rank", nullable = false)
    private Integer rank;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String wireframe;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problem;

    @ElementCollection
    @CollectionTable(
            name = "optimization_recommendation_changes",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            indexes = {
                    @Index(
                            name = "idx_opt_rec_changes_rec_order",
                            columnList = "recommendation_id, item_order"
                    )
            }
    )
    @OrderColumn(name = "item_order")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> whatToChange = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String implementationDirection;

    @ElementCollection
    @CollectionTable(
            name = "optimization_recommendation_copy_directions",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            indexes = {
                    @Index(
                            name = "idx_opt_rec_copy_dirs_rec_order",
                            columnList = "recommendation_id, item_order"
                    )
            }
    )
    @OrderColumn(name = "item_order")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> copyDirection = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String layoutDirection;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedBehaviorChange;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String riskOrCaveat;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private RecommendationUsageStatus usageStatus = RecommendationUsageStatus.UNUSED;

    public SectionOptimizationRecommendation(Section section, OptimizationRecommendationRequest recommendation) {
        this.section = section;
        this.rank = recommendation.rank();
        this.title = recommendation.title();
        this.wireframe = normalizeWireframe(recommendation.wireframe());
        this.problem = recommendation.problem();
        this.whatToChange = new ArrayList<>(recommendation.what_to_change());
        this.implementationDirection = recommendation.implementation_direction();
        this.copyDirection = new ArrayList<>(recommendation.copy_direction());
        this.layoutDirection = recommendation.layout_direction();
        this.expectedBehaviorChange = recommendation.expected_behavior_change();
        this.riskOrCaveat = recommendation.risk_or_caveat();
    }

    public void markUsed() {
        this.usageStatus = RecommendationUsageStatus.USED;
    }

    public void markUnused() {
        this.usageStatus = RecommendationUsageStatus.UNUSED;
    }

    public RecommendationUsageStatus getUsageStatus() {
        return usageStatus == null ? RecommendationUsageStatus.UNUSED : usageStatus;
    }

    public OptimizationRecommendationResponse toResponse() {
        return new OptimizationRecommendationResponse(
                id,
                getUsageStatus(),
                rank,
                title,
                normalizeWireframe(wireframe),
                problem,
                whatToChange,
                implementationDirection,
                copyDirection,
                layoutDirection,
                expectedBehaviorChange,
                riskOrCaveat
        );
    }

    private String normalizeWireframe(String wireframe) {
        return wireframe == null ? "" : wireframe;
    }
}
