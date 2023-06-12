package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMADetailedStrategicIndicators;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAQualityFactors;
import com.upc.gessi.qrapids.app.domain.exceptions.*;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Alert.AlertRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.MetricCategory.MetricCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QFCategory.QFCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QualityFactor.QualityFactorRepository;
import com.upc.gessi.qrapids.app.domain.repositories.SICategory.SICategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StrategicIndicator.StrategicIndicatorRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.presentation.rest.services.StrategicIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private StrategicIndicatorRepository siRepository;
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

    private Logger logger = LoggerFactory.getLogger(StrategicIndicators.class);


    public void createAlert(float value, float threshold, AlertType type, Project project, String affectedId, String affectedType) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        if (!checkAffectedIdExists(affectedId, affectedType, project.getId())) {
            if (affectedType.equals("metric")) throw new MetricNotFoundException();
            else if (affectedType.equals("factor")) throw new QualityFactorNotFoundException();
            else throw new StrategicIndicatorNotFoundException();
        }
        else {
            Alert newAlert = new Alert( value,  threshold,  type,  project,  affectedId, affectedType);
            saveAlert(newAlert);
        }
    }


    //METRIC ALERT CHECK
    public void shouldCreateMetricAlert (DTOMetricEvaluation m, float value, Long projectId) throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Metric metric = metricRepository.findByExternalIdAndProjectId(m.getId(), projectId);

        List<MetricCategory> metricCategoryLevels = metricCategoryRepository.findAllByName(metric.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (MetricCategory categoryValue:metricCategoryLevels) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }
        if(metric.getCategoryName()!=null && metric.getThreshold()!=null && !categoryThresholds.contains(metric.getThreshold())){
            checkMetricColorChangedAlert(metric, value, categoryThresholds);
            checkMetricThresholdTrespassedAlert(metric, value);
        }
        else if (metric.getCategoryName()!=null) checkMetricColorChangedAlert(metric, value, categoryThresholds); //ISSUE: the value to determine that it has not category is "Default" when it should be null to avoid problems
        else if (metric.getThreshold()!= null) checkMetricThresholdTrespassedAlert(metric, value);

    }

    void checkMetricThresholdTrespassedAlert(Metric metric, float value) throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        if (metric.getThreshold()!= null && value < metric.getThreshold()){
            Alert previousThresholdAlert= alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(metric.getProject().getId(),
                    metric.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(metric.getProject().getId(),
                    metric.getExternalId(), AlertType.ALERT_NOT_TREATED);


            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert!=null){
                Date lastAlertDate = lastAlert.getDate();

                LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate toDate = LocalDate.now();

                List<Float> metricEvaluationsValues = new ArrayList<>();
                List< DTOMetricEvaluation> metricEvaluations = null;
                metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(),
                        fromDate, toDate, metric.getProject().getExternalId(), null);

                for (DTOMetricEvaluation metricEvaluation : metricEvaluations) {
                    metricEvaluationsValues.add(metricEvaluation.getValue());
                }

                Date today = new Date();
                long diff = today.getTime() - lastAlertDate.getTime();
                boolean isAlertNotTreated = isATrespassedThresholdNotTreated(metric.getThreshold(), metricEvaluationsValues);
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, metric.getThreshold(),
                        AlertType.ALERT_NOT_TREATED, metric.getProject(), metric.getExternalId(), "metric");
                else if (!isAlertNotTreated) createAlert(value, metric.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric");
            }
            else createAlert(value, metric.getThreshold(),
                    AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric");
        }
    }
    void checkMetricColorChangedAlert(Metric metric, float value, List<Float> metricCategoryThresholds) throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
    LocalDate toDate = LocalDate.now();
    LocalDate fromDate = toDate.minusDays(30);

        List<DTOMetricEvaluation> metricEvaluations = new ArrayList<>();
        try{
            metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(),
                    fromDate, toDate, metric.getProject().getExternalId(), null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        if(!metricEvaluations.isEmpty()){
            DTOMetricEvaluation lastEvaluation = metricEvaluations.get(0);
            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue(), metricCategoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value, metricCategoryThresholds);

            if (lastEvaluation.getValue() > value && previousCategoryLevel!=currentCategoryLevel ){
                createAlert(value, metric.getThreshold() != null ? metric.getThreshold() : Float.NaN, AlertType.CATEGORY_DOWNGRADE, metric.getProject(),
                        metric.getExternalId(), "metric");
            }
            else if (lastEvaluation.getValue() < value && previousCategoryLevel!=currentCategoryLevel){
                createAlert(value, metric.getThreshold() != null ? metric.getThreshold() : Float.NaN, AlertType.CATEGORY_UPGRADE, metric.getProject(),
                        metric.getExternalId(), "metric");
            }

            else{
                Alert previousDowngradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(metric.getProject().getId(),
                    metric.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Alert previousUpgradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(metric.getProject().getId(),
                        metric.getExternalId(), AlertType.CATEGORY_UPGRADE);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(metric.getProject().getId(),
                        metric.getExternalId(), AlertType.ALERT_NOT_TREATED);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert!=null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, metric.getThreshold() != null ? metric.getThreshold() : Float.NaN, AlertType.ALERT_NOT_TREATED, metric.getProject(),
                            metric.getExternalId(), "metric");
                }
            }
        }
    }

    //QUALITY FACTOR ALERT CHECK
    public void shouldCreateFactorAlert (Factor factor, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        List<QFCategory> qfCategories = qfCategoryRepository.findAllByName(factor.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (QFCategory categoryValue:qfCategories) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }

        if (factor.getCategoryName()!=null && factor.getThreshold()!=null && !categoryThresholds.contains(factor.getThreshold())){
            checkFactorColorChangedAlert(factor, value, categoryThresholds);
            checkFactorThresholdTrespassedAlert(factor, value);
        }
        else if (factor.getCategoryName()!=null) checkFactorColorChangedAlert(factor, value, categoryThresholds); //ISSUE: the value to determine that it has not category is "Default" when it should be null to avoid problems
        else if (factor.getThreshold()!= null) checkFactorThresholdTrespassedAlert(factor, value);

    }

    private void checkFactorColorChangedAlert(Factor factor, float value, List<Float> categoryThresholds) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now();
        List<DTODetailedFactorEvaluation> factorEvaluations = new ArrayList<>();
        try{
            factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(),
                    fromDate, toDate, factor.getProject().getExternalId(), null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        if (!factorEvaluations.isEmpty()) {
            DTODetailedFactorEvaluation lastEvaluation = factorEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(), categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value, categoryThresholds);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel != currentCategoryLevel) {
                createAlert(value, factor.getThreshold() != null ? factor.getThreshold() : Float.NaN, AlertType.CATEGORY_DOWNGRADE, factor.getProject(),
                        factor.getExternalId(), "factor");
            } else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel != currentCategoryLevel) {
                createAlert(value, factor.getThreshold() != null ? factor.getThreshold() : Float.NaN, AlertType.CATEGORY_UPGRADE, factor.getProject(),
                        factor.getExternalId(), "factor");
            } else {
                Alert previousDowngradeAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(factor.getProject().getId(),
                        factor.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Alert previousUpgradeAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(factor.getProject().getId(),
                        factor.getExternalId(), AlertType.CATEGORY_UPGRADE);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(factor.getProject().getId(),
                        factor.getExternalId(), AlertType.ALERT_NOT_TREATED);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert!=null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value,
                            factor.getThreshold() != null ? factor.getThreshold() : Float.NaN, AlertType.ALERT_NOT_TREATED,
                            factor.getProject(), factor.getExternalId(), "factor");
                }
            }
        }
    }

    private void checkFactorThresholdTrespassedAlert(Factor factor, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        if (value < factor.getThreshold()){
            Alert previousThresholdAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(factor.getProject().getId(),
                    factor.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(factor.getProject().getId(),
                    factor.getExternalId(), AlertType.ALERT_NOT_TREATED);

            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert!=null){
                Date lastAlertDate = lastAlert.getDate();

                LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate toDate = LocalDate.now();
                List<Float> factorEvaluationValues = new ArrayList<>();

                List<DTODetailedFactorEvaluation> factorEvaluations = null;
                factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(), fromDate, toDate, factor.getProject().getExternalId(), null);
                for (DTODetailedFactorEvaluation factorEvaluation : factorEvaluations) {
                    factorEvaluationValues.add(factorEvaluation.getValue().getFirst());
                }

                Date today = new Date();
                long diff = today.getTime() - lastAlertDate.getTime();
                boolean isAlertNotTreated = isATrespassedThresholdNotTreated(factor.getThreshold(),factorEvaluationValues);
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7){
                    createAlert(value, factor.getThreshold(),
                            AlertType.ALERT_NOT_TREATED, factor.getProject(), factor.getExternalId(), "factor");
                }
                else if (!isAlertNotTreated) createAlert(value, factor.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor");
            }
            else createAlert(value, factor.getThreshold(),
                    AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor");
        }
    }


    //STRATEGIC INDICATOR ALERT CHECK
    public void shouldCreateIndicatorAlert (Strategic_Indicator strategicIndicator, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
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

    private void checkSIColorChangedAlert(Strategic_Indicator strategicIndicator, float value, List<Float>categoryThresholds) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now();
        List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = new ArrayList<>();
        try{
            siEvaluations = qmaDetailedStrategicIndicators.HistoricalData
                    (strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(),
                            null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        if (!siEvaluations.isEmpty()){
            DTODetailedStrategicIndicatorEvaluation lastEvaluation = siEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(),categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value,categoryThresholds);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel!=currentCategoryLevel ){
                createAlert(value, strategicIndicator.getThreshold() != null ? strategicIndicator.getThreshold() : Float.NaN,
                        AlertType.CATEGORY_DOWNGRADE, strategicIndicator.getProject(), strategicIndicator.getExternalId(),
                        "indicator");
            }
            else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel!=currentCategoryLevel){
                createAlert(value, strategicIndicator.getThreshold() != null ? strategicIndicator.getThreshold() : Float.NaN,
                        AlertType.CATEGORY_UPGRADE, strategicIndicator.getProject(), strategicIndicator.getExternalId(),
                        "indicator");
            }
            else {
                Alert previousDowngradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(strategicIndicator.getProject().getId(),
                        strategicIndicator.getExternalId(), AlertType.CATEGORY_DOWNGRADE);
                Alert previousUpgradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(strategicIndicator.getProject().getId(),
                        strategicIndicator.getExternalId(), AlertType.CATEGORY_UPGRADE);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(strategicIndicator.getProject().getId(),
                        strategicIndicator.getExternalId(), AlertType.ALERT_NOT_TREATED);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert!=null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) createAlert(value, strategicIndicator.getThreshold() != null ? strategicIndicator.getThreshold() : Float.NaN,
                            AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(), strategicIndicator.getExternalId(),
                            "indicator");
                }
            }
        }


    }

    private void checkSIThresholdTrespassedAlert(Strategic_Indicator strategicIndicator, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        if (value < strategicIndicator.getThreshold()){
            Alert previousThresholdAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(strategicIndicator.getProject().getId(),
                    strategicIndicator.getExternalId(), AlertType.TRESPASSED_THRESHOLD);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndTypeOrderByIdDesc(strategicIndicator.getProject().getId(),
                    strategicIndicator.getExternalId(), AlertType.ALERT_NOT_TREATED);

            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert!=null){
                Date lastAlertDate = lastAlert.getDate();

                LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate toDate = LocalDate.now();

                List<Float> siEvaluationsValues = new ArrayList<>();
                List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = null;
                    siEvaluations = qmaDetailedStrategicIndicators.HistoricalData(
                            strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(),
                            null);

                for (DTODetailedStrategicIndicatorEvaluation eval : siEvaluations) {
                    siEvaluationsValues.add(eval.getValue().getFirst());
                }

                Date today = new Date();
                long diff = today.getTime() - lastAlertDate.getTime();
                boolean isAlertNotTreated = isATrespassedThresholdNotTreated(strategicIndicator.getThreshold(), siEvaluationsValues);
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7){
                    createAlert(value, strategicIndicator.getThreshold(),
                            AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
                }
                else if (!isAlertNotTreated) {
                    createAlert(value, strategicIndicator.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
                }
            }
            else createAlert(value, strategicIndicator.getThreshold(),
                    AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator");
        }
    }


     boolean isATrespassedThresholdNotTreated(float threshold, List<Float> previousEvaluationsValues){
        boolean improvedSinceLastAlert = false;
        for (Float evalValue : previousEvaluationsValues) {
            if (evalValue >= threshold) {
                improvedSinceLastAlert = true;
                break;
            }
        }
        return !improvedSinceLastAlert;
    }

    int findCategoryLevel(float value, List<Float> categoryThresholds) {
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

    public Alert getAlertById(Long alertId) throws AlertNotFoundException {
        Alert alert = alertRepository.findAlertById(alertId);
        if (alert == null) throw new AlertNotFoundException();
        return alert;
    }

    private void saveAlert(Alert alert){
        alertRepository.save(alert);
    }

    public Boolean checkAffectedIdExists(String affectedId, String affectedType, Long projectId){
        if (affectedType.equals("metric")) return metricRepository.findByExternalIdAndProjectId(affectedId, projectId)!=null;
        else if (affectedType.equals("factor")) return factorRepository.findByExternalIdAndProjectId(affectedId,projectId)!=null;
        else if (affectedType.equals("indicator")) return siRepository.findByExternalIdAndProjectId(affectedId,projectId)!=null;
        return false;
    }

    public Alert obtainMostRecentAlert(Alert firstAlert, Alert secondAlert){
        if (firstAlert!=null && secondAlert!=null){
            Date firstDate = firstAlert.getDate();
            Date secondDate= secondAlert.getDate();
            if (firstDate.compareTo(secondDate)>0) return firstAlert;
            else if (firstDate.compareTo(secondDate)<0) return secondAlert;
            else{
                if(firstAlert.getType()==AlertType.ALERT_NOT_TREATED) return firstAlert;
                else if (secondAlert.getType()==AlertType.ALERT_NOT_TREATED) return secondAlert;
                else return firstAlert; //default, should never happen
            }
        }
        else if (firstAlert!=null) return firstAlert;
        else if (secondAlert!=null) return secondAlert;
        else return null;
    }
    public Alert obtainMostRecentAlert(Alert firstAlert, Alert secondAlert, Alert thirdAlert){
        if (firstAlert!=null && secondAlert!=null && thirdAlert!=null){
            Alert recentAlert = obtainMostRecentAlert(firstAlert,secondAlert);
            Date recentAlertDate = recentAlert.getDate();
            Date thirdAlertDate = thirdAlert.getDate();
            if (thirdAlertDate.compareTo(recentAlertDate)>0) return thirdAlert;
            else if (thirdAlertDate.compareTo(recentAlertDate)<0) return recentAlert;
            else{
                if(thirdAlert.getType()==AlertType.ALERT_NOT_TREATED) return thirdAlert;
                else if (recentAlert.getType()==AlertType.ALERT_NOT_TREATED) return recentAlert;
                else return thirdAlert; //default, should never happen
            }
        }
        else if (secondAlert!=null && thirdAlert!=null) return obtainMostRecentAlert(secondAlert,thirdAlert);
        else if (firstAlert!=null && secondAlert!=null) return obtainMostRecentAlert(firstAlert,secondAlert);
        else if (firstAlert!=null && thirdAlert!=null) return obtainMostRecentAlert(firstAlert,thirdAlert);
        else if (firstAlert!=null) return firstAlert;
        else if (secondAlert!=null) return secondAlert;
        else if (thirdAlert!=null) return thirdAlert;
        else return null;
    }

}
