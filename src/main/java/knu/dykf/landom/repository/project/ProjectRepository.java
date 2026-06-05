package knu.dykf.landom.repository.project;

import knu.dykf.landom.entity.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUserId(Long userId);
    boolean existsByApiKey(String apiKey);
    Optional<Project> findByApiKey(String apiKey);
}
