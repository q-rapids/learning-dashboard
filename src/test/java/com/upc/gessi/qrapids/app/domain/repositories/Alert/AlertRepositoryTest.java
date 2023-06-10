package com.upc.gessi.qrapids.app.domain.repositories.Alert;

import com.upc.gessi.qrapids.app.domain.models.Alert;
import com.upc.gessi.qrapids.app.domain.models.AlertStatus;
import com.upc.gessi.qrapids.app.domain.models.AlertType;
import com.upc.gessi.qrapids.app.domain.models.Project;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class AlertRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private AlertRepository alertRepository;
    @Test
    public void findAlertById() {
        // Given
        Alert alert = new Alert(0.30f,  0.33f,  AlertType.TRESPASSED_THRESHOLD,  null,  "badBacklogManagement", "metric", null, null);
        entityManager.persistAndFlush(alert);

        // When
        Alert alertFound = alertRepository.findAlertById(alert.getId());

        // Then
        assertNotNull(alertFound);
        assertEquals(alert.getId(), alertFound.getId());
    }

    @Test
    public void findAllByProjectIdWhenHasAlerts(){
        //Given
        Project project = new Project("test_project", "TestProject", "", null, true, null,null,false);
        entityManager.persistAndFlush(project);
        Long projectId = project.getId();

        Alert alert1 = new Alert(0.30f,  0.33f,  AlertType.TRESPASSED_THRESHOLD,  project,  "badBacklogManagement", "metric", null, null);
        Alert alert2 = new Alert(0.28f,  0.33f,  AlertType.ALERT_NOT_TREATED,  project,  "badBacklogManagement", "metric", null, null);
        entityManager.persistAndFlush(alert1);
        entityManager.persistAndFlush(alert2);

        //When
        List<Alert> projectAlerts = alertRepository.findAllByProjectId(projectId);

        //Then
        assertFalse(projectAlerts.isEmpty());
        assertEquals(2, projectAlerts.size());
        assertEquals(projectId, projectAlerts.get(0).getProject().getId(), projectAlerts.get(1).getProject().getId());
    }

    @Test
    public void findAllByProjectIdWithoutAlerts(){
        //Given
        Project project = new Project("test_project2", "TestProject2", "", null, true, null,null,false);
        entityManager.persistAndFlush(project);
        Long projectId = project.getId();

        //When
        List<Alert> projectAlerts = alertRepository.findAllByProjectId(projectId);

        //Then
        assertTrue(projectAlerts.isEmpty());
    }

    @Test
    public void findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(){
        //Given
        Project project = new Project("test_project3", "TestProject3", "", null, true, null,null,false);
        entityManager.persistAndFlush(project);
        Long projectId = project.getId();

        Alert alert = new Alert(0.30f,  0.50f,  AlertType.TRESPASSED_THRESHOLD,  project,  "badBacklogManagement", "metric", null, null);
        Alert alert2 = new Alert(0.00f,  0.50f,  AlertType.ALERT_NOT_TREATED,  project,  "badBacklogManagement", "metric", null, null);
        Alert alert3 = new Alert(0.30f,  0.33f,  AlertType.TRESPASSED_THRESHOLD,  project,  "acceptance_criteria_check", "metric", null, null);
        entityManager.persistAndFlush(alert);
        entityManager.persistAndFlush(alert2);
        entityManager.persistAndFlush(alert3);

        //When
        Alert alertFound = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(projectId, "badBacklogManagement", AlertType.TRESPASSED_THRESHOLD);

        //Then
        assertEquals(alertFound.getProject().getId(),projectId);
        assertEquals(alertFound.getAffectedId(),"badBacklogManagement");
        assertEquals(alertFound.getType(),AlertType.TRESPASSED_THRESHOLD);
    }

    @Test
    public void countByProjectIdAndStatus(){
        //Given
        Project project = new Project("test_project3", "TestProject3", "", null, true, null,null,false);
        entityManager.persistAndFlush(project);
        Long projectId = project.getId();

        Alert alert = new Alert(0.31f,  0.50f,  AlertType.TRESPASSED_THRESHOLD,  project,  "badBacklogManagement", "metric", null, null);
        Alert alert2 = new Alert(0.01f,  0.50f,  AlertType.ALERT_NOT_TREATED,  project,  "badBacklogManagement", "metric", null, null);
        Alert alert3 = new Alert(0.31f,  0.33f,  AlertType.TRESPASSED_THRESHOLD,  project,  "acceptance_criteria_check", "metric", null, null);
        entityManager.persistAndFlush(alert);
        entityManager.persistAndFlush(alert2);
        entityManager.persistAndFlush(alert3);

        //When
        int count = alertRepository.countByProjectIdAndStatus(projectId, AlertStatus.NEW);

        //Then
        assertEquals(3, count);
    }

    @Test
    public void setStatusToViewed(){
        // Given
        Alert alert = new Alert(0.31f,  0.50f,  AlertType.TRESPASSED_THRESHOLD,  null,  "badBacklogManagement", "metric", null, null);
        entityManager.persistAndFlush(alert);

        // When
        alertRepository.setViewedStatus(alert.getId());

        // Then
        Alert viewedAlert = entityManager.find(Alert.class, alert.getId());
        assertEquals(viewedAlert.getStatus(), AlertStatus.VIEWED);
    }

    @Test
    public void findTodayExactAlert(){
        //Given
        Project project = new Project("test_project", "TestProject", "", null, true, null,null,false);
        entityManager.persistAndFlush(project);
        Long projectId = project.getId();
        Date predDate = new Date();
        Alert alert = new Alert(0.31f,  0.50f,  AlertType.TRESPASSED_THRESHOLD,  project,  "badBacklogManagement", "metric", predDate, "PROPHET");
        entityManager.persistAndFlush(alert);
        LocalDate todayDate= LocalDate.now();
        LocalDateTime todayStart = todayDate.atStartOfDay();
        Date startDate= Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());
        Date now = new Date();
        // When
        Alert alertFound = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndValueAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThanAndThreshold(project.getId(),"badBacklogManagement","metric", 0.31f, AlertType.TRESPASSED_THRESHOLD,"PROPHET",predDate, startDate, now, 0.50f);

        // Then
        assertNotNull(alertFound);
        assertEquals(alert.getId(), alertFound.getId());
    }
}
