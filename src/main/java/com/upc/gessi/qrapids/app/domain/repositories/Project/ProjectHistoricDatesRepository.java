package com.upc.gessi.qrapids.app.domain.repositories.Project;

import com.upc.gessi.qrapids.app.domain.models.ProjectHistoricDates;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProjectHistoricDatesRepository extends CrudRepository<ProjectHistoricDates, Long> {
    List<ProjectHistoricDates> findByProject(Long projectId);
}
