package com.upc.gessi.qrapids.app.domain.repositories.Alert;

import com.upc.gessi.qrapids.app.domain.models.Alert;
import com.upc.gessi.qrapids.app.domain.models.AlertStatus;
import com.upc.gessi.qrapids.app.domain.models.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAll();
    Alert findAlertById(Long id);
    List<Alert> findAllByProjectId(Long projectId);
    Alert findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(Long projectId, String affectedId, String affectedType, AlertType type, Date startDayDate);
    Alert findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(
            Long projectId, String affectedId, String affectedType, AlertType type, String technique, Date predictionDate, Date startDayDate, Date nowDate);
    Alert findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
            Long projectId, String affectedId, String affectedType, AlertType type, Date startDayDate, Date nowDate);
    int countByProjectIdAndStatus(Long projectId, AlertStatus status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Alert a set a.status = 1 where a.status = 0 and a.id in ?1")
    void setViewedStatus(Long alertId);
}

