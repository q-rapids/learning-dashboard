package com.upc.gessi.qrapids.app.domain.repositories.StudentIdentity;

import com.upc.gessi.qrapids.app.domain.models.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
public class StudentIdentityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentIdentityRepository studentIdentityRepository;

    @Test
    public void findAllByStudent() {
        // Given

        String studentName = "test";
        String username = "test";
        Student student = new Student(studentName, null);
        StudentIdentity studentIdentity = new StudentIdentity(DataSource.Github, username, student);

        List<StudentIdentity> studentIdentities = Collections.singletonList(studentIdentity);
        entityManager.persistAndFlush(student);
        entityManager.persistAndFlush(studentIdentity);


        // When
        List<StudentIdentity> studentIdentitiesFound = studentIdentityRepository.findAllByStudent(student);

        // Then
        assertEquals(studentIdentities, studentIdentitiesFound);
    }
}