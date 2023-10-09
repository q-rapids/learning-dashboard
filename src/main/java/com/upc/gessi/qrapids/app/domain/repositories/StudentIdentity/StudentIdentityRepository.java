package com.upc.gessi.qrapids.app.domain.repositories.StudentIdentity;

import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.models.StudentIdentity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StudentIdentityRepository extends CrudRepository<StudentIdentity, Long> {

    List<StudentIdentity> findAllByStudent(Student student);
}
