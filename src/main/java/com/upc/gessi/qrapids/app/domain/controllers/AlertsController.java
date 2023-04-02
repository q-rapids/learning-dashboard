package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMADetailedStrategicIndicators;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAQualityFactors;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAStrategicIndicators;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Alert.AlertRepository;
import com.upc.gessi.qrapids.app.domain.repositories.MetricCategory.MetricCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QFCategory.QFCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.SICategory.SICategoryRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODetailedFactorEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODetailedStrategicIndicatorEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMetricEvaluation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AlertsController {

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private MetricCategoryRepository metricCategoryRepository;
    @Autowired
    private QFCategoryRepository qfCategoryRepository;

    @Autowired
    private SICategoryRepository siCategoryRepository;

    @Autowired
    private QMAMetrics qmaMetrics;
    @Autowired
    private QMAQualityFactors qmaFactors;

    @Autowired
    private QMADetailedStrategicIndicators qmaDetailedStrategicIndicators;

    private void createAlert(float value, float threshold, AlertType type, Project project, String affectedId, String affectedType){
        Alert newAlert = new Alert( value,  threshold,  type,  project,  affectedId, affectedType);
        saveAlert(newAlert);
    }
    
    public void shouldCreateMetricAlert (Metric metric, float value){

        List<MetricCategory> metricCategories = new ArrayList<>();
        Iterable<MetricCategory> metricCategoryIterable = metricCategoryRepository.findAll();
        metricCategoryIterable.forEach(metricCategories::add);

        if (!metricCategories.isEmpty() && metric.getCategoryName()!="Default") checkMetricColorChangedAlert(metric); //ISSUE: You can create a category with name default
        else checkMetricThresholdTrespassedAlert(metric, value);
    }

    private void checkMetricThresholdTrespassedAlert(Metric metric, float value) {
        if (metric.getThreshold()!= null && value < metric.getThreshold()){
            if (!isAMetricAlertNotTreated(metric, value)) createAlert(value, metric.getThreshold(), AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric");
        }
    }

    private void checkMetricColorChangedAlert(Metric metric) {

    }

    private boolean isAMetricAlertNotTreated(Metric metric, float value) {
        List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdOrderByIdAsc(metric.getProject().getId(),metric.getExternalId());
        Alert lastAlert = previousAlerts.get(previousAlerts.size()-1);
        Date lastAlertDate = lastAlert.getDate();

        boolean improvedSinceLastAlert = false;
        LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = LocalDate.now();

        try {
            List< DTOMetricEvaluation> metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(), fromDate, toDate, metric.getProject().getExternalId(), null);
            for (DTOMetricEvaluation metricEvaluation : metricEvaluations) {
                //this is for thresholds, what if it has categories?
                if (metricEvaluation.getValue() >= metric.getThreshold()) {
                    improvedSinceLastAlert = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Date todayDate = new Date();
        long diff = todayDate.getTime() - lastAlertDate.getTime();

        if (!improvedSinceLastAlert && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 3){
            createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(), metric.getExternalId(), "metric");
        }

        return !improvedSinceLastAlert;
    }

    public void shouldCreateFactorAlert (Factor factor, float value){
        List<QFCategory> qfCategories = new ArrayList<>();
        Iterable<QFCategory>  qfCategoryIterable = qfCategoryRepository.findAll();
        qfCategoryIterable.forEach(qfCategories::add);

        if (!qfCategories.isEmpty()) checkFactorColorChangedAlert(factor);
        else checkFactorThresholdTrespassedAlert(factor, value);

    }

    private void checkFactorColorChangedAlert(Factor factor) {

    }

    private void checkFactorThresholdTrespassedAlert(Factor factor, float value) {
        if (value < factor.getThreshold()){
            if (!isAFactorAlertNotTreated(factor,value)) createAlert(value, factor.getThreshold(), AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor");
        }
    }

    private boolean isAFactorAlertNotTreated(Factor factor, float value) {
        List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdOrderByIdAsc(factor.getProject().getId(),factor.getExternalId());
        Alert lastAlert = previousAlerts.get(previousAlerts.size()-1);
        Date lastAlertDate = lastAlert.getDate();

        boolean improvedSinceLastAlert = false;
        LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = LocalDate.now();

        try {
            List<DTODetailedFactorEvaluation> factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(), fromDate, toDate, factor.getProject().getExternalId(), null);
            for (DTODetailedFactorEvaluation factorEvaluation : factorEvaluations) {
                //this is for thresholds, what if it has categories?
                if (factorEvaluation.getValue().getFirst() >= factor.getThreshold()) {
                    improvedSinceLastAlert = true;
                    break;
                }
            }
        } catch (IOException | ProjectNotFoundException e) {
            throw new RuntimeException(e);
        }

        Date todayDate = new Date();
        long diff = todayDate.getTime() - lastAlertDate.getTime();

        if (!improvedSinceLastAlert && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 3){
            createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(), factor.getExternalId(), "factor");
        }

        return !improvedSinceLastAlert;
    }

    public void shouldCreateIndicatorAlert (Strategic_Indicator strategicIndicator, float value){
        List<SICategory> SICategories = new ArrayList<>();
        Iterable<SICategory>  siCategoryIterable = siCategoryRepository.findAll();
        siCategoryIterable.forEach(SICategories::add);

        if (!SICategories.isEmpty()) checkSIColorChangedAlert(strategicIndicator);
        else checkSIThresholdTrespassedAlert(strategicIndicator, value);
    }

    private void checkSIColorChangedAlert(Strategic_Indicator strategicIndicator) {
        
    }

    private void checkSIThresholdTrespassedAlert(Strategic_Indicator strategicIndicator, float value) {
        if (value < strategicIndicator.getThreshold()){
            if (!isAnSIAlertNotTreated(strategicIndicator, value)) createAlert(value, strategicIndicator.getThreshold(), AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
        }
    }

    private boolean isAnSIAlertNotTreated(Strategic_Indicator strategicIndicator, float value) {
        List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdOrderByIdAsc(strategicIndicator.getProject().getId(),strategicIndicator.getExternalId());
        Alert lastAlert = previousAlerts.get(previousAlerts.size()-1);
        Date lastAlertDate = lastAlert.getDate();

        boolean improvedSinceLastAlert = false;
        LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = LocalDate.now();

        try {
            List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = qmaDetailedStrategicIndicators.HistoricalData(strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(), null);
            for (DTODetailedStrategicIndicatorEvaluation eval : siEvaluations) {
                //this is for thresholds, what if it has categories?
                if (eval.getValue().getFirst() >= strategicIndicator.getThreshold()) {
                    improvedSinceLastAlert = true;
                    break;
                }
            }
        } catch (IOException | ProjectNotFoundException e) {
            throw new RuntimeException(e);
        }

        Date todayDate = new Date();
        long diff = todayDate.getTime() - lastAlertDate.getTime();

        if (!improvedSinceLastAlert && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 3){
            createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
        }

        return !improvedSinceLastAlert;
    }

    public void changeAlertStatusToViewed(Long alertId){
        Alert alert = getAlertById(alertId);
        alertRepository.setViewedStatus(alert);
    }
    
    public List<Alert>  getAllAlerts(){
        return alertRepository.findAll();
    }

    public List<Alert> getAllProjectAlerts(Long projectId){
        return alertRepository.findAllByProjectId(projectId);
    }

    public Alert getAlertById(Long alertId){
        return alertRepository.findAlertById(alertId);
    }

    private void saveAlert(Alert alert){
        alertRepository.save(alert);
    }

}
