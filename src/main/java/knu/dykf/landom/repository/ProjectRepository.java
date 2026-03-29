package knu.dykf.landom.repository;

import knu.dykf.landom.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByUserId(Long userId);
}
