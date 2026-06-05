package knu.dykf.landom.entity.project;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sections",
        indexes = {
                @Index(name = "idx_sections_project_step", columnList = "project_id, step_order")
        }
)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionName name;

    @Column(nullable = false)
    private String cssSelector;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String html;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String cssRules;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedHtml;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String generatedCss;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CodeGenerationStatus codeGenerationStatus = CodeGenerationStatus.CODE_NOT_GENERATED;

    private LocalDateTime codeGeneratedAt;

    @Column(nullable = false)
    private int stepOrder; // 퍼널 순서 (1, 2, 3...)

    @Builder
    public Section(Project project, SectionName name, String cssSelector, String html, String cssRules, int stepOrder) {
        this.project = project;
        this.name = name;
        this.cssSelector = cssSelector;
        this.html = html;
        this.cssRules = cssRules;
        this.stepOrder = stepOrder;
    }

    public void updateSource(String html, String cssRules) {
        this.html = html;
        this.cssRules = cssRules;
    }

    public void updateGeneratedCode(String html, String css) {
        this.generatedHtml = html;
        this.generatedCss = css;
        this.codeGenerationStatus = CodeGenerationStatus.CODE_GENERATED;
        this.codeGeneratedAt = LocalDateTime.now();
    }

    public void markCodeGenerationInProgress() {
        this.codeGenerationStatus = CodeGenerationStatus.IN_PROGRESS;
        this.codeGeneratedAt = null;
    }

    public void resetGeneratedCode() {
        this.generatedHtml = null;
        this.generatedCss = null;
        this.codeGenerationStatus = CodeGenerationStatus.CODE_NOT_GENERATED;
        this.codeGeneratedAt = null;
    }

    public CodeGenerationStatus getCodeGenerationStatus() {
        return codeGenerationStatus == null
                ? CodeGenerationStatus.CODE_NOT_GENERATED
                : codeGenerationStatus;
    }
}
