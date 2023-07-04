package com.upc.gessi.qrapids.app.domain.repositories.ProjectIdentity;

import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.ProjectIdentityRepository.ProjectIdentityRepository;
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
public class ProjectIdentityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectIdentityRepository projectIdentityRepository;

    @Test
    public void findAllByProject() {
        // Given


        String externalId = "test";
        String name = "test";
        String description = "test";
        String url = "test";
        Project project = new Project(externalId, name, description, null, true, true);
        ProjectIdentity projectIdentity = new ProjectIdentity(DataSource.GITHUB, url, project);

        List<ProjectIdentity> projectIdentities = Collections.singletonList(projectIdentity);
        entityManager.persistAndFlush(project);
        entityManager.persistAndFlush(projectIdentity);


        // When
        List<ProjectIdentity> projectIdentitiesFound = projectIdentityRepository.findAllByProject(project);

        // Then
        assertEquals(projectIdentities, projectIdentitiesFound);
    }
}