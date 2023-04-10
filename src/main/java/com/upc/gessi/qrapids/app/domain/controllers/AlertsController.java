package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMADetailedStrategicIndicators;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAQualityFactors;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Alert.AlertRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.MetricCategory.MetricCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QFCategory.QFCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QualityFactor.QualityFactorRepository;
import com.upc.gessi.qrapids.app.domain.repositories.SICategory.SICategoryRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AlertsController {

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private MetricRepository metricRepository;
    @Autowired
    private QualityFactorRepository factorRepository;
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


    public void createAlert(float value, float threshold, AlertType type, Project project, String affectedId, String affectedType){
        Alert newAlert = new Alert( value,  threshold,  type,  project,  affectedId, affectedType);
        saveAlert(newAlert);
    }


    //METRIC ALERT CHECK
    public void shouldCreateMetricAlert (DTOMetricEvaluation m, float value, Long projectId){
        Metric metric = metricRepository.findByExternalIdAndProjectId(m.getId(), projectId);

        List<MetricCategory> metricCategoryLevels = metricCategoryRepository.findAllByName(metric.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (MetricCategory categoryValue:metricCategoryLevels) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }

        if(!metric.getCategoryName().equals("Default") && metric.getThreshold()!=null && !categoryThresholds.contains(metric.getThreshold())){
            checkMetricColorChangedAlert(metric, value, categoryThresholds);
            checkMetricThresholdTrespassedAlert(metric, value);
        }
        else if (!metric.getCategoryName().equals("Default")) checkMetricColorChangedAlert(metric, value, categoryThresholds); //ISSUE: the value to determine that it has not category is "Default" when it should be null to avoid problems
        else if (metric.getThreshold()!= null) checkMetricThresholdTrespassedAlert(metric, value);

    }

    private void checkMetricThresholdTrespassedAlert(Metric metric, float value) {
        if (metric.getThreshold()!= null && value < metric.getThreshold()){
            List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(metric.getProject().getId(),
                    metric.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert lastAlert = previousAlerts.get(0);
            Date lastAlertDate = lastAlert.getDate();

            LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate toDate = LocalDate.now();

            List<Float> metricEvaluationsValues = new ArrayList<>();
            List< DTOMetricEvaluation> metricEvaluations = null;
            try {
                metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(),
                        fromDate, toDate, metric.getProject().getExternalId(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (DTOMetricEvaluation metricEvaluation : metricEvaluations) {
                metricEvaluationsValues.add(metricEvaluation.getValue());
            }

            if (isATrespassedThresholdNotTreated(lastAlertDate,metric.getThreshold(), metricEvaluationsValues)) createAlert(value, metric.getThreshold(),
                    AlertType.ALERT_NOT_TREATED, metric.getProject(), metric.getExternalId(), "metric");
            else createAlert(value, metric.getThreshold(),
                    AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric");
        }
    }

    private void checkMetricColorChangedAlert(Metric metric, float value, List<Float> metricCategoryThresholds) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(1);

        try {
            List<DTOMetricEvaluation> metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(),
                    fromDate, toDate, metric.getProject().getExternalId(), null);
            DTOMetricEvaluation lastEvaluation = metricEvaluations.get(0);

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue(), metricCategoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value, metricCategoryThresholds);

            if (lastEvaluation.getValue() > value && previousCategoryLevel!=currentCategoryLevel ){
                createAlert(value, metric.getThreshold(), AlertType.CATEGORY_DOWNGRADE, metric.getProject(),
                        metric.getExternalId(), "metric");
            }
            else if (lastEvaluation.getValue() < value && previousCategoryLevel!=currentCategoryLevel){
                createAlert(value, metric.getThreshold(), AlertType.CATEGORY_UPGRADE, metric.getProject(),
                        metric.getExternalId(), "metric");
            }
            else {
                List<Alert> previousDowngradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(metric.getProject().getId(),
                        metric.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                List<Alert> previousUpgradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(metric.getProject().getId(),
                        metric.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Date lastDowngradeAlertDate = previousDowngradeAlerts.get(0).getDate();
                Date lastUpgradeAlertDate = previousUpgradeAlerts.get(0).getDate();

                Date todayDate = new Date();
                long diff = todayDate.getTime() - lastDowngradeAlertDate.getTime();

                if (lastUpgradeAlertDate.before(lastDowngradeAlertDate) && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(),
                        metric.getExternalId(), "metric");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //QUALITY FACTOR ALERT CHECK
    public void shouldCreateFactorAlert (Factor factor, float value){
        List<QFCategory> qfCategories = qfCategoryRepository.findAllByName(factor.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (QFCategory categoryValue:qfCategories) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }

        if(!factor.getCategoryName().equals("Default") && factor.getThreshold()!=null && !categoryThresholds.contains(factor.getThreshold())){
            checkFactorColorChangedAlert(factor, value, categoryThresholds);
            checkFactorThresholdTrespassedAlert(factor, value);
        }
        else if (!factor.getCategoryName().equals("Default")) checkFactorColorChangedAlert(factor, value, categoryThresholds); //ISSUE: the value to determine that it has not category is "Default" when it should be null to avoid problems
        else if (factor.getThreshold()!= null) checkFactorThresholdTrespassedAlert(factor, value);

    }

    private void checkFactorColorChangedAlert(Factor factor, float value, List<Float> categoryThresholds) {
        LocalDate fromDate = LocalDate.now().minusDays(1);
        LocalDate toDate = LocalDate.now();
        try {
            List<DTODetailedFactorEvaluation> factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(),
                    fromDate, toDate, factor.getProject().getExternalId(), null);
            DTODetailedFactorEvaluation lastEvaluation = factorEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(),categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value,categoryThresholds);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel!=currentCategoryLevel ){
                createAlert(value, factor.getThreshold(), AlertType.CATEGORY_DOWNGRADE, factor.getProject(),
                        factor.getExternalId(), "factor");
            }
            else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel!=currentCategoryLevel){
                createAlert(value, factor.getThreshold(), AlertType.CATEGORY_UPGRADE, factor.getProject(),
                        factor.getExternalId(), "factor");
            }
            else {
                List<Alert> previousDowngradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(factor.getProject().getId(),
                        factor.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                List<Alert> previousUpgradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(factor.getProject().getId(),
                        factor.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Date lastDowngradeAlertDate = previousDowngradeAlerts.get(0).getDate();
                Date lastUpgradeAlertDate = previousUpgradeAlerts.get(0).getDate();
                Date todayDate = new Date();
                long diff = todayDate.getTime() - lastDowngradeAlertDate.getTime();

                if (lastUpgradeAlertDate.before(lastDowngradeAlertDate) && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(),
                        factor.getExternalId(), "factor");
            }

        } catch (IOException | ProjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkFactorThresholdTrespassedAlert(Factor factor, float value) {
        if (value < factor.getThreshold()){
            List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(factor.getProject().getId(),
                    factor.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert lastAlert = previousAlerts.get(0);
            Date lastAlertDate = lastAlert.getDate();

            LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate toDate = LocalDate.now();
            List<Float> factorEvaluationValues = new ArrayList<>();

            List<DTODetailedFactorEvaluation> factorEvaluations = null;
            try {
                factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(),
                        fromDate, toDate, factor.getProject().getExternalId(), null);
            } catch (IOException | ProjectNotFoundException e) {
                throw new RuntimeException(e);
            }
            for (DTODetailedFactorEvaluation factorEvaluation : factorEvaluations) {
                factorEvaluationValues.add(factorEvaluation.getValue().getFirst());
            }
            if (isATrespassedThresholdNotTreated(lastAlertDate, factor.getThreshold(),factorEvaluationValues)){
                createAlert(value, factor.getThreshold(),
                        AlertType.ALERT_NOT_TREATED, factor.getProject(), factor.getExternalId(), "factor");
            }
            else createAlert(value, factor.getThreshold(),
                    AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor");
        }
    }


    //STRATEGIC INDICATOR ALERT CHECK
    public void shouldCreateIndicatorAlert (Strategic_Indicator strategicIndicator, float value){
        List<SICategory> SICategories = new ArrayList<>();
        Iterable<SICategory>  siCategoryIterable = siCategoryRepository.findAll();
        siCategoryIterable.forEach(SICategories::add);
        List<Float> categoryThresholds = Arrays.asList(1.0f,0.67f,0.33f);

        if(!SICategories.isEmpty() && strategicIndicator.getThreshold()!=null && !categoryThresholds.contains(strategicIndicator.getThreshold())){
            checkSIColorChangedAlert(strategicIndicator, value, categoryThresholds);
            checkSIThresholdTrespassedAlert(strategicIndicator, value);
        }
        else if (!SICategories.isEmpty()) checkSIColorChangedAlert(strategicIndicator, value, categoryThresholds); //ISSUE: the value to determine that it has not category is "Default" when it should be null to avoid problems
        else if (strategicIndicator.getThreshold()!= null) checkSIThresholdTrespassedAlert(strategicIndicator, value);
    }

    private void checkSIColorChangedAlert(Strategic_Indicator strategicIndicator, float value, List<Float>categoryThresholds) {
        LocalDate fromDate = LocalDate.now().minusDays(1);
        LocalDate toDate = LocalDate.now();
        try {
            List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = qmaDetailedStrategicIndicators.HistoricalData
                    (strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(),
                            null);
            DTODetailedStrategicIndicatorEvaluation lastEvaluation = siEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(),categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value,categoryThresholds);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel!=currentCategoryLevel ){
                createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_DOWNGRADE, strategicIndicator.getProject(),
                        strategicIndicator.getExternalId(), "indicator");
            }
            else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel!=currentCategoryLevel){
                createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_UPGRADE, strategicIndicator.getProject(),
                        strategicIndicator.getExternalId(), "indicator");
            }
            else {
                List<Alert> previousDowngradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(strategicIndicator.getProject().getId(),
                        strategicIndicator.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                List<Alert> previousUpgradeAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(strategicIndicator.getProject().getId(),
                        strategicIndicator.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Date lastDowngradeAlertDate = previousDowngradeAlerts.get(0).getDate();
                Date lastUpgradeAlertDate = previousUpgradeAlerts.get(0).getDate();
                Date todayDate = new Date();
                long diff = todayDate.getTime() - lastDowngradeAlertDate.getTime();

                if (lastUpgradeAlertDate.before(lastDowngradeAlertDate) && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(),
                        strategicIndicator.getExternalId(), "indicator");
            }

        } catch (IOException | ProjectNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkSIThresholdTrespassedAlert(Strategic_Indicator strategicIndicator, float value) {
        if (value < strategicIndicator.getThreshold()){
            List<Alert> previousAlerts= alertRepository.findAllByProjectIdAndAffectedIdAndTypeOrderByDateDesc(strategicIndicator.getProject().getId(),
                    strategicIndicator.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert lastAlert = previousAlerts.get(0);
            Date lastAlertDate = lastAlert.getDate();

            LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate toDate = LocalDate.now();

            List<Float> siEvaluationsValues = new ArrayList<>();
            List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = null;
            try {
                siEvaluations = qmaDetailedStrategicIndicators.HistoricalData(
                        strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(),
                        null);
            } catch (IOException | ProjectNotFoundException e) {
                throw new RuntimeException(e);
            }

            for (DTODetailedStrategicIndicatorEvaluation eval : siEvaluations) {
                siEvaluationsValues.add(eval.getValue().getFirst());
            }

            if (isATrespassedThresholdNotTreated(lastAlertDate, strategicIndicator.getThreshold(), siEvaluationsValues)){
                createAlert(value, strategicIndicator.getThreshold(),
                        AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
            }
            else {
                createAlert(value, strategicIndicator.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
            }
        }
    }


    private boolean isATrespassedThresholdNotTreated(Date lastAlertDate, float threshold, List<Float> previousEvaluationsValues){
        boolean improvedSinceLastAlert = false;
        Date today = new Date();
        long diff = today.getTime() - lastAlertDate.getTime();
        for (Float evalValue : previousEvaluationsValues) {
            if (evalValue >= threshold) {
                improvedSinceLastAlert = true;
                break;
            }
        }
        return !improvedSinceLastAlert && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7;
    }

    private int findCategoryLevel(float value, List<Float> categoryThresholds) {
        boolean levelFound = false;
        int level = -1;
        for (int i = categoryThresholds.size() - 1; i >= 0 && !levelFound; i--) {
            if (value <= categoryThresholds.get(i)) {
                level = i + 1;
                levelFound = true;
            }
        }
        return level;
    }

    //ACCESS TO DB METHODS
    public void changeAlertStatusToViewed(Alert alert){
        alertRepository.setViewedStatus(alert.getId());
    }

    public int countNewAlerts(Long projectId){return alertRepository.countByProjectIdAndStatus(projectId, AlertStatus.NEW);}

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
