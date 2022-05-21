package com.upc.gessi.qrapids.app.domain.repositories.Student;

import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StudentRepository extends CrudRepository<Student, Long> {
    List<Student> findAllByProjectId(Long projectId);

    void deleteAllByProjectId(Long projectId);

}
