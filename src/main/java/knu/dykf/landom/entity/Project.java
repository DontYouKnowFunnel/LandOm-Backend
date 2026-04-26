package knu.dykf.landom.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    private String generateApiKey() {
        return UUID.randomUUID().toString();
    }

    @Builder
    public Project(User user, String name, String description) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.apiKey = generateApiKey();
    }
}
