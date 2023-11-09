package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMADetailedStrategicIndicators;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAQualityFactors;
import com.upc.gessi.qrapids.app.domain.exceptions.*;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Alert.AlertRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.MetricCategory.MetricCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QFCategory.QFCategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QualityFactor.QualityFactorRepository;
import com.upc.gessi.qrapids.app.domain.repositories.SICategory.SICategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StrategicIndicator.StrategicIndicatorQualityFactorsRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StrategicIndicator.StrategicIndicatorRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODetailedFactorEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODetailedStrategicIndicatorEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMetricEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.services.StrategicIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
    private StrategicIndicatorQualityFactorsRepository dsiRepository;
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
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProfilesController profilesController;


    private Logger logger = LoggerFactory.getLogger(StrategicIndicators.class);


    public void createAlert(float value, Float threshold, AlertType type, Project project, String affectedId, String affectedType, Date predictionDate, String predictionTechnique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        if (!checkAffectedIdExists(affectedId, affectedType, project.getId())) {
            if (affectedType.equals("metric")) throw new MetricNotFoundException();
            else if (affectedType.equals("factor")) throw new QualityFactorNotFoundException();
            else throw new StrategicIndicatorNotFoundException();
        }
        else {
            Alert newAlert = new Alert(value,  threshold,  type,  project,  affectedId, affectedType, predictionDate, predictionTechnique);
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

    private void checkMetricThresholdTrespassedAlert(Metric metric, float value) throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.ALERT_NOT_TREATED, todayStartDate, now);
        Alert alreadyCreated_TT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.TRESPASSED_THRESHOLD, todayStartDate, now);

        if (metric.getThreshold() != null && value < metric.getThreshold()){
            Alert previousThresholdAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.TRESPASSED_THRESHOLD, todayStartDate);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.ALERT_NOT_TREATED, todayStartDate);

            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert != null){
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
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                    if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
                    if (alreadyCreated_NT == null) createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(),
                            metric.getExternalId(), "metric", null, null);
                    else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), metric.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_NT.getId());
                        createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(),
                                metric.getExternalId(), "metric", null, null);
                    }
                }
                else if (!isAlertNotTreated) {
                    if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
                    if (alreadyCreated_TT == null) createAlert(value, metric.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric", null, null);
                    else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), metric.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_TT.getId());
                        createAlert(value, metric.getThreshold(),
                                AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric", null, null);
                    }
                }
            }
            else {
                if (alreadyCreated_TT == null) createAlert(value, metric.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric", null, null);
                else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), metric.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_TT.getId());
                    createAlert(value, metric.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, metric.getProject(), metric.getExternalId(), "metric", null, null);
                }
            }
        }
        else {
            if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
            if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
        }
    }

    private void checkMetricColorChangedAlert(Metric metric, float value, List<Float> metricCategoryThresholds) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        LocalDate Date = LocalDate.now();
        LocalDate toDate = Date.minusDays(1);
        LocalDate fromDate = toDate.minusDays(30);

        List<DTOMetricEvaluation> metricEvaluations = new ArrayList<>();
        try{
            metricEvaluations = qmaMetrics.SingleHistoricalData(metric.getExternalId(),
                    fromDate, toDate, metric.getProject().getExternalId(), null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        if(!metricEvaluations.isEmpty()){
            DTOMetricEvaluation lastEvaluation = metricEvaluations.get(0);
            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue(), metricCategoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value, metricCategoryThresholds);

            Alert alreadyCreated_CU = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_UPGRADE, todayStartDate, now);
            Alert alreadyCreated_CD = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_DOWNGRADE, todayStartDate, now);
            Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.ALERT_NOT_TREATED, todayStartDate, now);

            if (lastEvaluation.getValue() > value && previousCategoryLevel != currentCategoryLevel) {

                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CD == null) createAlert(value, metric.getThreshold(), AlertType.CATEGORY_DOWNGRADE, metric.getProject(),
                        metric.getExternalId(), "metric", null, null);
                else if (alreadyCreated_CD.getValue() != value || !Objects.equals(alreadyCreated_CD.getThreshold(), metric.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CD.getId());
                    createAlert(value, metric.getThreshold(), AlertType.CATEGORY_DOWNGRADE, metric.getProject(),
                            metric.getExternalId(), "metric", null, null);

                }
            }

            else if (lastEvaluation.getValue() < value && previousCategoryLevel != currentCategoryLevel) {

                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CU == null) createAlert(value, metric.getThreshold(), AlertType.CATEGORY_UPGRADE, metric.getProject(),
                        metric.getExternalId(), "metric", null,null);
                else if (alreadyCreated_CU.getValue() != value || !Objects.equals(alreadyCreated_CU.getThreshold(), metric.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CU.getId());
                    createAlert(value, metric.getThreshold(), AlertType.CATEGORY_UPGRADE, metric.getProject(),
                            metric.getExternalId(), "metric", null,null);
                }
            }

            else {

                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());

                Alert previousDowngradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_DOWNGRADE, todayStartDate);
                Alert previousUpgradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_UPGRADE, todayStartDate);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.ALERT_NOT_TREATED, todayStartDate);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert != null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                        if (alreadyCreated_NT == null) createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(),
                                metric.getExternalId(), "metric", null, null);
                        else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), metric.getThreshold())) {
                            alertRepository.deleteById(alreadyCreated_NT.getId());
                            createAlert(value, metric.getThreshold(), AlertType.ALERT_NOT_TREATED, metric.getProject(),
                                    metric.getExternalId(), "metric", null, null);
                        }
                    }
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
        LocalDate toDate = LocalDate.now().minusDays(1);
        List<DTODetailedFactorEvaluation> factorEvaluations = new ArrayList<>();
        try{
            factorEvaluations = qmaFactors.HistoricalData(factor.getExternalId(),
                    fromDate, toDate, factor.getProject().getExternalId(), null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        if (!factorEvaluations.isEmpty()) {
            DTODetailedFactorEvaluation lastEvaluation = factorEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(), categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value, categoryThresholds);

            Alert alreadyCreated_CU = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.CATEGORY_UPGRADE, todayStartDate, now);
            Alert alreadyCreated_CD = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.CATEGORY_DOWNGRADE, todayStartDate, now);
            Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.ALERT_NOT_TREATED, todayStartDate, now);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel != currentCategoryLevel) {
                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CD == null) createAlert(value, factor.getThreshold(), AlertType.CATEGORY_DOWNGRADE, factor.getProject(),
                        factor.getExternalId(), "factor", null, null);
                else if (alreadyCreated_CD.getValue() != value || !Objects.equals(alreadyCreated_CD.getThreshold(), factor.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CD.getId());
                    createAlert(value, factor.getThreshold(), AlertType.CATEGORY_DOWNGRADE, factor.getProject(),
                            factor.getExternalId(), "factor", null, null);

                }
            }
            else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel != currentCategoryLevel) {
                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CU == null) createAlert(value, factor.getThreshold(), AlertType.CATEGORY_UPGRADE, factor.getProject(),
                        factor.getExternalId(), "factor", null,null);
                else if (alreadyCreated_CU.getValue() != value || !Objects.equals(alreadyCreated_CU.getThreshold(), factor.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CU.getId());
                    createAlert(value, factor.getThreshold(), AlertType.CATEGORY_UPGRADE, factor.getProject(),
                            factor.getExternalId(), "factor", null,null);
                }
            }
            else {
                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());

                Alert previousDowngradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.CATEGORY_DOWNGRADE, todayStartDate);
                Alert previousUpgradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.CATEGORY_UPGRADE, todayStartDate);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.ALERT_NOT_TREATED, todayStartDate);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert != null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                        if (alreadyCreated_NT == null) createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(),
                                factor.getExternalId(), "factor", null, null);
                        else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), factor.getThreshold())) {
                            alertRepository.deleteById(alreadyCreated_NT.getId());
                            createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(),
                                    factor.getExternalId(), "factor", null, null);
                        }
                    }
                }
            }
        }
    }

    private void checkFactorThresholdTrespassedAlert(Factor factor, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.ALERT_NOT_TREATED, todayStartDate, now);
        Alert alreadyCreated_TT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.TRESPASSED_THRESHOLD, todayStartDate, now);

        if (factor.getThreshold() != null && value < factor.getThreshold()){
            Alert previousThresholdAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.TRESPASSED_THRESHOLD, todayStartDate);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    factor.getProject().getId(), factor.getExternalId(), "factor", AlertType.ALERT_NOT_TREATED, todayStartDate);

            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert != null){
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
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                    if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
                    if (alreadyCreated_NT == null) createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(),
                            factor.getExternalId(), "factor", null, null);
                    else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), factor.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_NT.getId());
                        createAlert(value, factor.getThreshold(), AlertType.ALERT_NOT_TREATED, factor.getProject(),
                                factor.getExternalId(), "factor", null, null);
                    }
                }
                else if (!isAlertNotTreated) {
                    if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
                    if (alreadyCreated_TT == null) createAlert(value, factor.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor", null, null);
                    else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), factor.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_TT.getId());
                        createAlert(value, factor.getThreshold(),
                                AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor", null, null);
                    }
                }
            }
            else {
                if (alreadyCreated_TT == null) createAlert(value, factor.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor", null, null);
                else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), factor.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_TT.getId());
                    createAlert(value, factor.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, factor.getProject(), factor.getExternalId(), "factor", null, null);
                }
            }
        }
        else {
            if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
            if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
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
        LocalDate toDate = LocalDate.now().minusDays(1);
        List<DTODetailedStrategicIndicatorEvaluation> siEvaluations = new ArrayList<>();
        try{
            siEvaluations = qmaDetailedStrategicIndicators.HistoricalData
                    (strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(),
                            null);
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }

        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        if (!siEvaluations.isEmpty()){
            DTODetailedStrategicIndicatorEvaluation lastEvaluation = siEvaluations.get(0); //assuming most recents are first

            int previousCategoryLevel = findCategoryLevel(lastEvaluation.getValue().getFirst(),categoryThresholds);
            int currentCategoryLevel = findCategoryLevel(value,categoryThresholds);

            Alert alreadyCreated_CU = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.CATEGORY_UPGRADE, todayStartDate, now);
            Alert alreadyCreated_CD = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.CATEGORY_DOWNGRADE, todayStartDate, now);
            Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                    strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.ALERT_NOT_TREATED, todayStartDate, now);

            if (lastEvaluation.getValue().getFirst() > value && previousCategoryLevel != currentCategoryLevel) {
                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CD == null) createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_DOWNGRADE, strategicIndicator.getProject(),
                        strategicIndicator.getExternalId(), "indicator", null, null);
                else if (alreadyCreated_CD.getValue() != value || !Objects.equals(alreadyCreated_CD.getThreshold(), strategicIndicator.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CD.getId());
                    createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_DOWNGRADE, strategicIndicator.getProject(),
                            strategicIndicator.getExternalId(), "indicator", null, null);

                }
            }
            else if (lastEvaluation.getValue().getFirst() < value && previousCategoryLevel != currentCategoryLevel) {
                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());

                if (alreadyCreated_CU == null) createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_UPGRADE, strategicIndicator.getProject(),
                        strategicIndicator.getExternalId(), "indicator", null,null);
                else if (alreadyCreated_CU.getValue() != value || !Objects.equals(alreadyCreated_CU.getThreshold(), strategicIndicator.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_CU.getId());
                    createAlert(value, strategicIndicator.getThreshold(), AlertType.CATEGORY_UPGRADE, strategicIndicator.getProject(),
                            strategicIndicator.getExternalId(), "indicator", null,null);
                }
            }
            else {
                if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
                if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());

                Alert previousDowngradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.CATEGORY_DOWNGRADE, todayStartDate);
                Alert previousUpgradeAlert= alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.CATEGORY_UPGRADE, todayStartDate);
                Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                        strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.ALERT_NOT_TREATED, todayStartDate);
                Alert lastAlert = obtainMostRecentAlert(previousDowngradeAlert, previousUpgradeAlert, previousNotTreatedAlert);

                if (lastAlert != null && (lastAlert.getType() == AlertType.CATEGORY_DOWNGRADE || lastAlert.getType() == AlertType.ALERT_NOT_TREATED)){
                    Date todayDate = new Date();
                    long diff = todayDate.getTime() - lastAlert.getDate().getTime();
                    if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                        if (alreadyCreated_NT == null) createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(),
                                strategicIndicator.getExternalId(), "indicator", null, null);
                        else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), strategicIndicator.getThreshold())) {
                            alertRepository.deleteById(alreadyCreated_NT.getId());
                            createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(),
                                    strategicIndicator.getExternalId(), "indicator", null, null);
                        }
                    }
                }
            }
        }

    }

    private void checkSIThresholdTrespassedAlert(Strategic_Indicator strategicIndicator, float value) throws ProjectNotFoundException, IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        Alert alreadyCreated_NT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.ALERT_NOT_TREATED, todayStartDate, now);
        Alert alreadyCreated_TT = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.TRESPASSED_THRESHOLD, todayStartDate, now);

        if (strategicIndicator.getThreshold() != null && value < strategicIndicator.getThreshold()){
            Alert previousThresholdAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.TRESPASSED_THRESHOLD, todayStartDate);
            Alert previousNotTreatedAlert = alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                    strategicIndicator.getProject().getId(), strategicIndicator.getExternalId(), "indicator", AlertType.ALERT_NOT_TREATED, todayStartDate);

            Alert lastAlert = obtainMostRecentAlert(previousThresholdAlert, previousNotTreatedAlert);

            if (lastAlert != null){
                Date lastAlertDate = lastAlert.getDate();

                LocalDate fromDate = lastAlertDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate toDate = LocalDate.now();
                List<Float> factorEvaluationValues = new ArrayList<>();

                List<DTODetailedFactorEvaluation> factorEvaluations = null;
                factorEvaluations = qmaFactors.HistoricalData(strategicIndicator.getExternalId(), fromDate, toDate, strategicIndicator.getProject().getExternalId(), null);
                for (DTODetailedFactorEvaluation factorEvaluation : factorEvaluations) {
                    factorEvaluationValues.add(factorEvaluation.getValue().getFirst());
                }

                Date today = new Date();
                long diff = today.getTime() - lastAlertDate.getTime();
                boolean isAlertNotTreated = isATrespassedThresholdNotTreated(strategicIndicator.getThreshold(),factorEvaluationValues);
                if (isAlertNotTreated && TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) > 7) {
                    if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
                    if (alreadyCreated_NT == null) createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(),
                            strategicIndicator.getExternalId(), "indicator", null, null);
                    else if (alreadyCreated_NT.getValue() != value || !Objects.equals(alreadyCreated_NT.getThreshold(), strategicIndicator.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_NT.getId());
                        createAlert(value, strategicIndicator.getThreshold(), AlertType.ALERT_NOT_TREATED, strategicIndicator.getProject(),
                                strategicIndicator.getExternalId(), "indicator", null, null);
                    }
                }
                else if (!isAlertNotTreated) {
                    if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
                    if (alreadyCreated_TT == null) createAlert(value, strategicIndicator.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator", null, null);
                    else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), strategicIndicator.getThreshold())) {
                        alertRepository.deleteById(alreadyCreated_TT.getId());
                        createAlert(value, strategicIndicator.getThreshold(),
                                AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator", null, null);
                    }
                }
            }
            else {
                if (alreadyCreated_TT == null) createAlert(value, strategicIndicator.getThreshold(),
                        AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator", null, null);
                else if (alreadyCreated_TT.getValue() != value || !Objects.equals(alreadyCreated_TT.getThreshold(), strategicIndicator.getThreshold())) {
                    alertRepository.deleteById(alreadyCreated_TT.getId());
                    createAlert(value, strategicIndicator.getThreshold(),
                            AlertType.TRESPASSED_THRESHOLD, strategicIndicator.getProject(), strategicIndicator.getExternalId(), "indicator", null, null);
                }
            }
        }
        else {
            if (alreadyCreated_NT != null) alertRepository.deleteById(alreadyCreated_NT.getId());
            if (alreadyCreated_TT != null) alertRepository.deleteById(alreadyCreated_TT.getId());
        }
    }

    boolean isATrespassedThresholdNotTreated(Float threshold, List<Float> previousEvaluationsValues){
        boolean improvedSinceLastAlert = false;
        for (Float evalValue : previousEvaluationsValues) {
            if (evalValue >= threshold) {
                improvedSinceLastAlert = true;
                break;
            }
        }
        return !improvedSinceLastAlert;
    }

    int findCategoryLevel(Float value, List<Float> categoryThresholds) {
        boolean levelFound = false;
        int level = -1;
        for (int i = categoryThresholds.size() - 1; i >= 0 && !levelFound; i--) {
            if (value != null && value <= categoryThresholds.get(i)) {
                level = i + 1;
                levelFound = true;
            }
        }
        return level;
    }

    //CHECK ALERTS FOR PREDICTION
    public void checkAlertsForMetricsPrediction(DTOMetricEvaluation currentEval, List<DTOMetricEvaluation> forecast, String projectExternalId, String technique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Project project = projectRepository.findByExternalId(projectExternalId);
        Metric metric = metricRepository.findByExternalIdAndProjectId(currentEval.getId(), project.getId());

        List<MetricCategory> metricCategoryLevels = metricCategoryRepository.findAllByName(metric.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (MetricCategory categoryValue:metricCategoryLevels) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }
        boolean alertCreated=false;
        //for each forecasted value, until the first alert created, we check if it has trespassed the threshold or a category (depending on if ti has categories and/or threshold)
        for(int i = 0; i < forecast.size() && !alertCreated; ++i) {
            LocalDate predictedDate = forecast.get(i).getDate();
            Date date;
            if (predictedDate==null) date = null;
            else date = java.sql.Date.valueOf(predictedDate);

            if(metric.getCategoryName()!=null && metric.getThreshold()!=null && !categoryThresholds.contains(metric.getThreshold())){
                boolean categoryAlertCreated = checkPredictionColorChangedAlert(currentEval.getValue(), forecast.get(i).getValue(), date, metric.getThreshold(), metric.getExternalId(), "metric", project, categoryThresholds, technique);
                boolean thresholdAlertCreated = checkPredictionThresholdTrespassedAlert(currentEval.getValue(), forecast.get(i).getValue(), date, metric.getThreshold(), metric.getExternalId(), "metric", project, technique);
                alertCreated =  categoryAlertCreated || thresholdAlertCreated;
            }
            else if (metric.getCategoryName()!=null) alertCreated = checkPredictionColorChangedAlert(currentEval.getValue(), forecast.get(i).getValue(), date, metric.getThreshold(), metric.getExternalId(), "metric", project, categoryThresholds, technique);
            else if (metric.getThreshold()!= null) alertCreated = checkPredictionThresholdTrespassedAlert(currentEval.getValue(), forecast.get(i).getValue(), date, metric.getThreshold(), metric.getExternalId(), "metric", project, technique);
        }


    }

    public void checkAlertsForFactorsPrediction(Float currentValue, String id, List<Float> predictedValues, List<Date> predictionDates, String projectExternalId, String technique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Project project = projectRepository.findByExternalId(projectExternalId);
        Factor factor = factorRepository.findByExternalIdAndProjectId(id, project.getId());

        List<QFCategory> qfCategoryLevels = qfCategoryRepository.findAllByName(factor.getCategoryName());
        List<Float> categoryThresholds = new ArrayList<>();
        for (QFCategory categoryValue:qfCategoryLevels) {
            categoryThresholds.add(categoryValue.getUpperThreshold());
        }
        boolean alertCreated=false;
        //for each forecasted value, until the first alert created, we check if it has trespassed the threshold or a category (depending on if ti has categories and/or threshold)
        for(int i = 0; i < predictedValues.size() && !alertCreated; ++i) {
            if(factor.getCategoryName()!=null && factor.getThreshold()!=null && !categoryThresholds.contains(factor.getThreshold())){
                boolean categoryAlertCreated = checkPredictionColorChangedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), factor.getThreshold(), factor.getExternalId(), "factor", project, categoryThresholds, technique);
                boolean thresholdAlertCreated = checkPredictionThresholdTrespassedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), factor.getThreshold(), factor.getExternalId(), "factor", project, technique);
                alertCreated =  categoryAlertCreated || thresholdAlertCreated;
            }
            else if (factor.getCategoryName()!=null) alertCreated = checkPredictionColorChangedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), factor.getThreshold(), factor.getExternalId(), "factor", project, categoryThresholds, technique);
            else if (factor.getThreshold()!= null) alertCreated = checkPredictionThresholdTrespassedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), factor.getThreshold(), factor.getExternalId(), "factor", project, technique);
        }
    }

    public void checkAlertsForIndicatorsPrediction(Float currentValue, String id, List<Float> predictedValues, List<Date> predictionDates, String projectExternalId, String technique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        Project project = projectRepository.findByExternalId(projectExternalId);
        Strategic_Indicator si = siRepository.findByExternalIdAndProjectId(id, project.getId());

        List<SICategory> SICategories = new ArrayList<>();
        Iterable<SICategory>  siCategoryIterable = siCategoryRepository.findAll();
        siCategoryIterable.forEach(SICategories::add);
        List<Float> categoryThresholds = Arrays.asList(1.0f,0.67f,0.33f);

        boolean alertCreated=false;
        //for each forecasted value, until the first alert created, we check if it has trespassed the threshold or a category (depending on if ti has categories and/or threshold)
        for(int i = 0; i < predictedValues.size() && !alertCreated; ++i) {
            if(!SICategories.isEmpty() && si.getThreshold()!=null && !categoryThresholds.contains(si.getThreshold())){
                boolean categoryAlertCreated = checkPredictionColorChangedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), si.getThreshold(), si.getExternalId(), "indicator", project, categoryThresholds, technique);
                boolean thresholdAlertCreated = checkPredictionThresholdTrespassedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), si.getThreshold(), si.getExternalId(), "indicator", project, technique);
                alertCreated =  categoryAlertCreated || thresholdAlertCreated;
            }
            else if (!SICategories.isEmpty()) alertCreated = checkPredictionColorChangedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), si.getThreshold(), si.getExternalId(), "indicator", project, categoryThresholds, technique);
            else if (si.getThreshold()!= null) alertCreated = checkPredictionThresholdTrespassedAlert(currentValue, predictedValues.get(i), predictionDates.get(i), si.getThreshold(), si.getExternalId(), "indicator", project, technique);
        }
    }

    private boolean checkPredictionColorChangedAlert(Float currentValue, Float predictedValue, Date predictionDate, Float threshold, String affectedId, String affectedType, Project project, List<Float> categoryThresholds, String technique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        int previousCategoryLevel = findCategoryLevel(currentValue, categoryThresholds);
        int predictedCategoryLevel = findCategoryLevel(predictedValue, categoryThresholds);
        boolean alertCreated= false;
        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        Alert alreadyCreated_CD = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(
                project.getId(), affectedId, affectedType, AlertType.PREDICTED_CATEGORY_DOWNGRADE, technique, predictionDate,  todayStartDate, now);

        Alert alreadyCreated_CU = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(
                project.getId(), affectedId, affectedType, AlertType.PREDICTED_CATEGORY_UPGRADE, technique, predictionDate,  todayStartDate, now);


        if (predictedValue!=null && predictedValue >= 0.f && currentValue > predictedValue && previousCategoryLevel!=predictedCategoryLevel ){
            if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());
            if (alreadyCreated_CD == null) createAlert(predictedValue, threshold, AlertType.PREDICTED_CATEGORY_DOWNGRADE, project,
                    affectedId, affectedType, predictionDate, technique);
            else if (!Objects.equals(alreadyCreated_CD.getThreshold(), threshold) || alreadyCreated_CD.getValue() != predictedValue) {
                alertRepository.deleteById(alreadyCreated_CD.getId());
                createAlert(predictedValue, threshold, AlertType.PREDICTED_CATEGORY_DOWNGRADE, project,
                        affectedId, affectedType, predictionDate, technique);
            }
            alertCreated = true;
        }

        else if (predictedValue!=null && predictedValue <= 1.f && currentValue < predictedValue && previousCategoryLevel!=predictedCategoryLevel) {
            if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
            if (alreadyCreated_CU == null) createAlert(predictedValue, threshold, AlertType.PREDICTED_CATEGORY_UPGRADE, project,
                    affectedId, affectedType,predictionDate, technique);
            else if (!Objects.equals(alreadyCreated_CU.getThreshold(), threshold) || alreadyCreated_CU.getValue() != predictedValue) {
                alertRepository.deleteById(alreadyCreated_CU.getId());
                createAlert(predictedValue, threshold, AlertType.PREDICTED_CATEGORY_UPGRADE, project,
                        affectedId, affectedType,predictionDate, technique);
            }
            alertCreated = true;
        }

        else {
            if (alreadyCreated_CU != null) alertRepository.deleteById(alreadyCreated_CU.getId());
            if (alreadyCreated_CD != null) alertRepository.deleteById(alreadyCreated_CD.getId());
        }

        return alertCreated;
    }

    private boolean checkPredictionThresholdTrespassedAlert(Float currentValue, Float predictedValue, Date predictionDate, Float threshold, String affectedId, String affectedType, Project project, String technique) throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        boolean alertCreated = false;
        Date todayStartDate = getTodayStartOfDayInstant();
        Date now = new Date();

        Alert alreadyCreated = alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(
                project.getId(), affectedId,affectedType, AlertType.PREDICTED_TRESPASSED_THRESHOLD, technique, predictionDate,  todayStartDate, now);

        if (threshold != null && predictedValue != null && predictedValue < threshold && currentValue >= threshold) {
            if (alreadyCreated == null)
                createAlert(predictedValue, threshold, AlertType.PREDICTED_TRESPASSED_THRESHOLD, project,
                        affectedId, affectedType, predictionDate, technique);

            else if (alreadyCreated.getValue() != predictedValue || !Objects.equals(alreadyCreated.getThreshold(), threshold)) {
                alertRepository.deleteById(alreadyCreated.getId());
                createAlert(predictedValue, threshold, AlertType.PREDICTED_TRESPASSED_THRESHOLD, project,
                        affectedId, affectedType, predictionDate, technique);
            }
            alertCreated = true;

        }
        else if (alreadyCreated != null) alertRepository.deleteById(alreadyCreated.getId());
        return alertCreated;
    }

    public Date getTodayStartOfDayInstant (){
        LocalDate todayDate= LocalDate.now();
        LocalDateTime todayStart = todayDate.atStartOfDay();
        return Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());
    }

    //ACCESS TO DB METHODS
    public void changeAlertStatusToViewed(Alert alert){
        alertRepository.setViewedStatus(alert.getId());
    }

    public int countNewAlerts(Long projectId) {
        return alertRepository.countByProjectIdAndStatus(projectId, AlertStatus.NEW);
    }

    public int countNewAlertsWithProfile(Long projectId, String profile){
        int newAlertsCount = alertRepository.countByProjectIdAndStatus(projectId, AlertStatus.NEW);
        if ( profile == null || profile.equals("null")){
            return newAlertsCount;
        }
        else {
            Profile prof = profilesController.findProfileById(profile);
            Project proj = projectRepository.findById(projectId).get();
            if (prof.getAllSIByProject(proj)) {
                return newAlertsCount;
            }
            else {
                List<Alert> newAlerts = alertRepository.findAllByProjectId(projectId);
                List<Alert> newFilteredAlerts = filterAlertsByProfileIndicatorsAndLevels(proj, prof, newAlerts);
                return newFilteredAlerts.size();
            }
        }
    }

    public List<Alert>  getAllAlerts(){
        return alertRepository.findAll();
    }

    public List<Alert> getAllProjectAlerts(Long projectId){
        return alertRepository.findAllByProjectId(projectId);
    }

    public List<Alert> getAllProjectAlertsWithProfile(Long projectId, String profile){
        List<Alert> alerts = alertRepository.findAllByProjectId(projectId);
        if ( profile == null || profile.equals("null")){
            return alerts;
        }
        else{ //filter alerts by profile
            Profile prof = profilesController.findProfileById(profile);
            Project proj = projectRepository.findById(projectId).get();
            if (prof.getAllSIByProject(proj)){
                if(prof.getQualityLevel().equals(Profile.QualityLevel.METRICS_FACTORS))
                    alerts.removeIf(alert -> alert.getAffectedType().equals("indicator"));
                else if (prof.getQualityLevel().equals(Profile.QualityLevel.METRICS)) {
                    alerts.removeIf(alert -> alert.getAffectedType().equals("indicator"));
                    alerts.removeIf(alert -> alert.getAffectedType().equals("factor"));
                }
                return alerts;
            } else {
                return filterAlertsByProfileIndicatorsAndLevels(proj,prof,alerts);
            }
        }
    }

    public List<Alert> filterAlertsByProfileIndicatorsAndLevels (Project project, Profile profile, List<Alert> alerts){
        List<ProfileProjectStrategicIndicators> siList = profile.getProfileProjectStrategicIndicatorsList();
        List<String> profileSIIds = new ArrayList<>();
        Set<String> profileQFIds = new HashSet<>();
        Set<String> profileMetricsIds = new HashSet<>();

        for (ProfileProjectStrategicIndicators si: siList) {
            profileSIIds.add(si.getStrategicIndicator().getExternalId());
            for(String f : si.getStrategicIndicator().getQuality_factors()){
                profileQFIds.add(f);
                Factor factor = factorRepository.findByExternalId(f).get();
                for(String m : factor.getMetrics()){
                    profileMetricsIds.add(m);
                }
            }
        }

        List<Alert> result = new ArrayList<>();
        Profile.QualityLevel ql = profile.getQualityLevel();
        for (Alert alert : alerts) {
            if (alert.getAffectedType().equals("indicator") && profileSIIds.contains(alert.getAffectedId()) && profile.getQualityLevel().equals(Profile.QualityLevel.ALL)){
                result.add(alert);
            }
            else if (alert.getAffectedType().equals("factor") && profileQFIds.contains(alert.getAffectedId()) && (profile.getQualityLevel().equals(Profile.QualityLevel.METRICS_FACTORS)) || profile.getQualityLevel().equals(Profile.QualityLevel.ALL)){
                result.add(alert);
            }
            else if (alert.getAffectedType().equals("metric") && profileMetricsIds.contains(alert.getAffectedId())){
                result.add(alert);
            }
        }
        return result;
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
