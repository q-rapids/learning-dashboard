package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Alert.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertsController {

    @Autowired
    private AlertRepository alertRepository;

    private void createAlert(float value, float threshold, AlertType type, Project project, String affectedId, String affectedType){
        Alert newAlert = new Alert( value,  threshold,  type,  project,  affectedId, affectedType);
        saveAlert(newAlert);
    }

    private void saveAlert(Alert alert){
        alertRepository.save(alert);
    }
    public void shouldCreateMetricAlert (Metric metric, float thresholdValue){
        if (thresholdValue < metric.getThreshold()){
            createAlert(thresholdValue, metric.getThreshold(), AlertType.CHANGED_COLORS, metric.getProject(), metric.getExternalId(), "metric");
            //TO DO: get/define alerttype
        }
    };
    public void shouldCreateFactorAlert (Factor factor, float thresholdValue){
        if (thresholdValue < factor.getThreshold()){
            createAlert(thresholdValue, factor.getThreshold(), AlertType.CHANGED_COLORS, factor.getProject(), factor.getExternalId(), "factor");
            //TO DO: get/define alerttype
        }
    };
    public void shouldCreateIndicatorAlert (Strategic_Indicator strategicIndicator, float thresholdValue){
        if (thresholdValue < strategicIndicator.getThreshold()){
            createAlert(thresholdValue, strategicIndicator.getThreshold(), AlertType.CHANGED_COLORS, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
            //TO DO: get/define alerttype
        }
    };

    public void changeAlertStatusToViewed(String alertId){
        Alert alert = getAlertById(alertId);
        alertRepository.setViewedStatus(alert);
    }
    public List<Alert>  getAllAlerts(){
        return alertRepository.findAll();
    }

    public List<Alert> getAllProjectAlerts(String projectId){
        return alertRepository.findAllByProjectId(projectId);
    }

    public Alert getAlertById(String alertId){
        return alertRepository.findAlertById(alertId);
    }
}
