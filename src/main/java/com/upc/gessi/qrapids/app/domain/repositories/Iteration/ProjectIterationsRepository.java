package com.upc.gessi.qrapids.app.domain.repositories.Iteration;

import com.upc.gessi.qrapids.app.domain.models.ProjectIterations;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProjectIterationsRepository extends CrudRepository<ProjectIterations, Long> {
    @Query("select p from ProjectIterations p where p.project_id = ?1")
    List<ProjectIterations> findByProject_id(Long projectId);

    @Query("select p from ProjectIterations p where p.date_id = ?1")
    List<ProjectIterations> findByDate_id(Long dateId);
    @Transactional
    @Modifying
    @Query("delete from ProjectIterations p where p.date_id = ?1")
    int deleteByDate_id(Long dateId);
}
