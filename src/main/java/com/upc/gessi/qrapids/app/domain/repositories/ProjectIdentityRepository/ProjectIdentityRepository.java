package com.upc.gessi.qrapids.app.domain.repositories.ProjectIdentityRepository;

import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.ProjectIdentity;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.models.StudentIdentity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProjectIdentityRepository extends CrudRepository<ProjectIdentity, Long> {

    List<ProjectIdentity> findAllByProject(Project project);
}
