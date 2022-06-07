package com.upc.gessi.qrapids.app.domain.repositories.Dates;

import com.upc.gessi.qrapids.app.domain.models.ProjectHistoricDates;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProjectHistoricDatesRepository extends CrudRepository<ProjectHistoricDates, Long> {
    @Query("select p from ProjectHistoricDates p where p.project_id = ?1")
    List<ProjectHistoricDates> findByProject_id(Long projectId);

    @Query("select p from ProjectHistoricDates p where p.date_id = ?1")
    List<ProjectHistoricDates> findByDate_id(Long dateId);
    @Transactional
    @Modifying
    @Query("delete from ProjectHistoricDates p where p.date_id = ?1")
    int deleteByDate_id(Long dateId);
}
