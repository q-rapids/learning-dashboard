package com.upc.gessi.qrapids.app.domain.repositories.Decision;

import com.upc.gessi.qrapids.app.domain.models.Decision;
import com.upc.gessi.qrapids.app.domain.models.Project;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
public class DecisionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DecisionRepository decisionRepository;

    @Test
    public void findByProject_Id() {
        // Given
        Project project = new Project("test", "Test", null, null, true, "testurl1", "testurl2");
        entityManager.persist(project);

        Decision decision = new Decision();
        decision.setProject(project);
        entityManager.persistAndFlush(decision);

        // When
        List<Decision> decisionsList = decisionRepository.findByProject_Id(project.getId());

        // Then
        int numberDecisionsFound = 1;
        assertEquals(numberDecisionsFound, decisionsList.size());
        assertEquals(decision, decisionsList.get(0));
    }
}