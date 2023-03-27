package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.models.Alert;
import com.upc.gessi.qrapids.app.domain.models.AlertType;
import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.Factor;
import com.upc.gessi.qrapids.app.domain.models.Strategic_Indicator;
import org.springframework.stereotype.Service;

@Service
public class AlertController {


    private void createAlert(float value, float threshold, AlertType type, String projectId, String affectedId){
        Alert newAlert = new Alert( value,  threshold,  type,  projectId,  affectedId);
        //save and send alert
    }
    public void shouldCreateMetricAlert (Metric metric, float thresholdValue){
        if (thresholdValue < metric.getThreshold()){
            createAlert(thresholdValue, metric.getThreshold(), AlertType.CHANGED_COLORS, metric.getProject().getExternalId(), metric.getExternalId());
            //TO DO: get/define alerttype
        }
    };
    public void shouldCreateFactorAlert (Factor factor, float thresholdValue){
        if (thresholdValue < factor.getThreshold()){
            createAlert(thresholdValue, factor.getThreshold(), AlertType.CHANGED_COLORS, factor.getProject().getExternalId(), factor.getExternalId());
            //TO DO: get/define alerttype
        }
    };
    public void shouldCreateIndicatorAlert (Strategic_Indicator strategicIndicator, float thresholdValue){
        if (thresholdValue < strategicIndicator.getThreshold()){
            createAlert(thresholdValue, strategicIndicator.getThreshold(), AlertType.CHANGED_COLORS, strategicIndicator.getProject().getExternalId(), strategicIndicator.getExternalId());
            //TO DO: get/define alerttype
        }
    };


    
}
