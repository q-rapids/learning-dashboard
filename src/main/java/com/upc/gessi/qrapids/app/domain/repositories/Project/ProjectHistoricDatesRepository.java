package com.upc.gessi.qrapids.app.domain.repositories.Project;

import com.upc.gessi.qrapids.app.domain.models.ProjectHistoricDate;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProjectHistoricDatesRepository extends CrudRepository<ProjectHistoricDate, Long> {
    List<ProjectHistoricDate> findByProject(Long projectId);
}
