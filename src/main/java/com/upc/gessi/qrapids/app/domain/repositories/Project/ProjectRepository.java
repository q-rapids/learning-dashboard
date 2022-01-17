package com.upc.gessi.qrapids.app.domain.repositories.Project;

import com.upc.gessi.qrapids.app.domain.models.Project;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;


public interface ProjectRepository extends CrudRepository<Project, Long> {

    Project findByExternalId(String externalId);

    List<Project> findAll();

    Project findByName(String name);

    Optional<Project> findById(Long Id);
}
