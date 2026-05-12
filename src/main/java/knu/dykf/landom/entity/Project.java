package knu.dykf.landom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N:1 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String description;

    private String url;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Enumerated(EnumType.STRING)
    private FunnelAnalysisStatus funnelAnalysisStatus = FunnelAnalysisStatus.NOT_CREATED;

    private LocalDateTime funnelAnalysisStartedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void updateInfo(String name, String description, String url) {
        this.name = name;
        this.description = description;
        this.url = url;
    }

    public FunnelAnalysisStatus getFunnelAnalysisStatus() {
        return funnelAnalysisStatus == null ? FunnelAnalysisStatus.NOT_CREATED : funnelAnalysisStatus;
    }

    public void startFunnelAnalysis() {
        this.funnelAnalysisStatus = FunnelAnalysisStatus.IN_PROGRESS;
        this.funnelAnalysisStartedAt = LocalDateTime.now();
    }

    public void completeFunnelAnalysis() {
        this.funnelAnalysisStatus = FunnelAnalysisStatus.COMPLETED;
        this.funnelAnalysisStartedAt = null;
    }

    public void failFunnelAnalysis() {
        this.funnelAnalysisStatus = FunnelAnalysisStatus.FAILED;
        this.funnelAnalysisStartedAt = null;
    }

    public void resetFunnelAnalysis() {
        this.funnelAnalysisStatus = FunnelAnalysisStatus.NOT_CREATED;
        this.funnelAnalysisStartedAt = null;
    }

    private String generateApiKey() {
        return UUID.randomUUID().toString();
    }

    @Builder
    public Project(User user, String name, String description, String url) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.url = url;
        this.apiKey = generateApiKey();
        this.funnelAnalysisStatus = FunnelAnalysisStatus.NOT_CREATED;
        this.funnelAnalysisStartedAt = null;
    }

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<Section> funnelSections = new ArrayList<>();

    public void updateFunnelSections(List<Section> newSections) {
        this.funnelSections.clear();
        this.funnelSections.addAll(newSections);
    }
}
