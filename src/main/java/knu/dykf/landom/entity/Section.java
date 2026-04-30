package knu.dykf.landom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Project와 N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String cssSelector;

    @Column(nullable = false)
    private int stepOrder; // 퍼널 순서 (1, 2, 3...)

    @Builder
    public Section(Project project, String name, String cssSelector, int stepOrder) {
        this.project = project;
        this.name = name;
        this.cssSelector = cssSelector;
        this.stepOrder = stepOrder;
    }
}