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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;
import knu.dykf.landom.dto.request.project.OptimizationRecommendationRequest;
import knu.dykf.landom.dto.response.project.OptimizationRecommendationResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "optimization_recommendations")
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problem;

    @ElementCollection
    @CollectionTable(
            name = "optimization_recommendation_changes",
            joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @OrderColumn(name = "item_order")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private List<String> whatToChange = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String implementationDirection;

    @ElementCollection
    @CollectionTable(
            name = "optimization_recommendation_copy_directions",
            joinColumns = @JoinColumn(name = "recommendation_id")
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

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedHtml;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedCss;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CodeGenerationStatus codeGenerationStatus = CodeGenerationStatus.CODE_NOT_GENERATED;

    public SectionOptimizationRecommendation(Section section, OptimizationRecommendationRequest recommendation) {
        this.section = section;
        this.rank = recommendation.rank();
        this.title = recommendation.title();
        this.problem = recommendation.problem();
        this.whatToChange = new ArrayList<>(recommendation.what_to_change());
        this.implementationDirection = recommendation.implementation_direction();
        this.copyDirection = new ArrayList<>(recommendation.copy_direction());
        this.layoutDirection = recommendation.layout_direction();
        this.expectedBehaviorChange = recommendation.expected_behavior_change();
        this.riskOrCaveat = recommendation.risk_or_caveat();
    }

    public void updateGeneratedCode(String html, String css) {
        this.generatedHtml = html;
        this.generatedCss = css;
        this.codeGenerationStatus = CodeGenerationStatus.CODE_GENERATED;
    }

    public CodeGenerationStatus getCodeGenerationStatus() {
        return codeGenerationStatus == null
                ? CodeGenerationStatus.CODE_NOT_GENERATED
                : codeGenerationStatus;
    }

    public OptimizationRecommendationResponse toResponse() {
        return new OptimizationRecommendationResponse(
                id,
                getCodeGenerationStatus(),
                rank,
                title,
                problem,
                whatToChange,
                implementationDirection,
                copyDirection,
                layoutDirection,
                expectedBehaviorChange,
                riskOrCaveat
        );
    }
}
