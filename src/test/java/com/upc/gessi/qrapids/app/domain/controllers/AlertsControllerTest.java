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
import com.upc.gessi.qrapids.app.testHelpers.DomainObjectsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.util.Pair;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AlertsControllerTest
{
    @InjectMocks
    private AlertsController alertsController;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private MetricRepository metricRepository;
    @Mock
    private MetricCategoryRepository metricCategoryRepository;
    @Mock
    private QMAMetrics qmaMetrics;
    @Mock
    private QFCategoryRepository QFCategoryRepository;
    @Mock
    private QualityFactorRepository factorRepository;
    @Mock
    private QMAQualityFactors qmaFactors;
    @Mock
    private SICategoryRepository siCategoryRepository;
    @Mock
    private StrategicIndicatorRepository siRepository;
    @Mock
    private QMADetailedStrategicIndicators qmaStrategicIndicators;
    private DomainObjectsBuilder domainObjectsBuilder;

    @Before
    public void setUp () {
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void createAlert() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        String affectedId = "duplication";
        String affectedType = "metric";
        AlertType type = AlertType.TRESPASSED_THRESHOLD;
        float value = 0.3f;
        float threshold = 0.67f;
        Project project = domainObjectsBuilder.buildProject();

        when(metricRepository.findByExternalIdAndProjectId(affectedId, project.getId())).thenReturn(new Metric());

        // When
        alertsController.createAlert(value, threshold, type, project, affectedId, affectedType);

        // Then
        ArgumentCaptor<Alert> alertArgCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgCaptor.capture());
        Alert alertCreated = alertArgCaptor.getValue();
        assertEquals(affectedType, alertCreated.getAffectedType());
        assertEquals(affectedId, alertCreated.getAffectedId());
        assertEquals(type, alertCreated.getType());
        assertEquals(value, alertCreated.getValue(),0.f);
        assertEquals(threshold, alertCreated.getThreshold(), 0.f);
        assertEquals(AlertStatus.NEW, alertCreated.getStatus());
        assertEquals(project, alertCreated.getProject());
    }

    @Test
    public void shouldCreateMetricAlertWithCategoryDowngrade() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        float value = 0.33f;
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);

        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //creating a previous evaluation that will have a higher value, so it will be in a different level and raise an alert
        DTOMetricEvaluation previousEval = domainObjectsBuilder.buildDTOMetric();
        List<DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval);
        when(qmaMetrics.SingleHistoricalData(eq(metric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateMetricAlert(metricEval, value, projectId);

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateMetricAlertWithCategoryUpgrade() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);

        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //creating a previous evaluation that will have a lower value, so it will be in a different level and raise an alert
        String metricId = "fasttests";
        String metricName = "Fast Tests";
        String metricDescription = "Percentage of tests under the testing duration threshold";
        float metricValue = 0.33f;
        LocalDate evaluationDate = LocalDate.now();
        String metricRationale = "parameters: {...}, formula: ...";
        String factorId = "testingperformance";
        List<String> qualityFactors = new ArrayList<>();
        qualityFactors.add(factorId);
        DTOMetricEvaluation previousEval= new DTOMetricEvaluation(metricId, metricName, metricDescription, null, metricRationale, qualityFactors, evaluationDate, metricValue);
        List <DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval);

        when(qmaMetrics.SingleHistoricalData(eq(metric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateMetricAlert(metricEval, metricEval.getValue(), projectId);

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(metricEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateMetricAlertWithThresholdTrespassed() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        float value = 0.3f; //threshold is 0.5f so the alert should be created at least for the threshold trespassed

        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setCategoryName("Default"); //we don't want it to have a category for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);

        // When
        alertsController.shouldCreateMetricAlert(metricEval, value, projectId);

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(metric.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateFactorAlertWithCategoryDowngrade() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        Factor factor = domainObjectsBuilder.buildFactor(project);
        factor.setThreshold(null); //we want it to not have a threshold for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);

        List<QFCategory> factorCategories = domainObjectsBuilder.buildFactorCategoryList();
        when(QFCategoryRepository.findAllByName(factor.getCategoryName())).thenReturn(factorCategories);

        //creating a previous evaluation that will have a higher value, so it will be in a different level and raise an alert
        DTODetailedFactorEvaluation previousEval = domainObjectsBuilder.buildDTOQualityFactor();
        previousEval.setValue(Pair.of(0.8f, "0.8"));
        List<DTODetailedFactorEvaluation> previousEvals = Arrays.asList(previousEval);
        when(qmaFactors.HistoricalData(eq(factor.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateFactorAlert(factor, 0.33f);

        // Then
        verify(factorRepository,times(1)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(0.33f, alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateFactorAlertWithCategoryUpgrade() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        Factor factor = domainObjectsBuilder.buildFactor(project);
        factor.setThreshold(null); //we want it to not have a threshold for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);
        List<QFCategory> factorCategories = domainObjectsBuilder.buildFactorCategoryList();
        when(QFCategoryRepository.findAllByName(factor.getCategoryName())).thenReturn(factorCategories);

        //creating a previous evaluation that will have a lower value, so it will be in a different level and raise an alert
        String factorId = "testingperformance";
        String factorName = "Testing Performance";
        String factorDescription = "Performance of the tests";

        String metricId = "fasttests";
        String metricName = "Fast Tests";
        String metricDescription = "Percentage of tests under the testing duration threshold";
        float metricValue = 0.3f;
        LocalDate evaluationDate = LocalDate.now();
        String metricRationale = "parameters: {...}, formula: ...";
        List<String> qualityFactors = new ArrayList<>();
        qualityFactors.add(factorId);
        DTOMetricEvaluation dtoMetricEvaluation = new DTOMetricEvaluation(metricId, metricName, metricDescription, null, metricRationale, qualityFactors, evaluationDate, metricValue);
        List<DTOMetricEvaluation> dtoMetricEvaluationList = new ArrayList<>();
        dtoMetricEvaluationList.add(dtoMetricEvaluation);
        DTODetailedFactorEvaluation previousEval = new DTODetailedFactorEvaluation(factorId, factorDescription, factorName, dtoMetricEvaluationList, "testType");
        previousEval.setValue(Pair.of(0.3f, "0.3"));
        List <DTODetailedFactorEvaluation> previousEvals = Arrays.asList(previousEval);

        when(qmaFactors.HistoricalData(eq(factor.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateFactorAlert(factor, 0.8f);

        // Then
        verify(factorRepository,times(1)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(0.8f, alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateFactorAlertWithThresholdTrespassed() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        Factor factor = domainObjectsBuilder.buildFactor(project);
        float value = 0.2f; //threshold is 0.3f so the alert should be created at least for the threshold trespassed
        factor.setCategoryName("Default"); //we don't want it to have a category for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);

        // When
        alertsController.shouldCreateFactorAlert(factor, value);

        // Then
        verify(factorRepository,times(1)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(factor.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateStrategicIndicatorAlertWithCategoryDowngrade() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();

        Strategic_Indicator strategic_indicator = domainObjectsBuilder.buildStrategicIndicator(project);
        strategic_indicator.setThreshold(null); //we want it to not have a threshold for this test
        when(siRepository.findByExternalIdAndProjectId(strategic_indicator.getExternalId(),project.getId())).thenReturn(strategic_indicator);

        List<SICategory> siCategories = domainObjectsBuilder.buildSICategoryList();
        when(siCategoryRepository.findAll()).thenReturn(siCategories);

        //creating a previous evaluation that will have a higher value, so it will be in a different level and raise an alert
        DTOFactorEvaluation dtoFactorEvaluation = domainObjectsBuilder.buildDTOFactor();
        List<DTOFactorEvaluation> dtoFactorList = new ArrayList<>();
        dtoFactorList.add(dtoFactorEvaluation);

        DTOStrategicIndicatorEvaluation dtoStrategicIndicatorEvaluation = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        DTODetailedStrategicIndicatorEvaluation  previousEval = new DTODetailedStrategicIndicatorEvaluation(dtoStrategicIndicatorEvaluation.getId(), dtoStrategicIndicatorEvaluation.getName(), dtoFactorList);
        previousEval.setValue(Pair.of(0.8f, "0.8"));
        List<DTODetailedStrategicIndicatorEvaluation> previousEvals = Arrays.asList(previousEval);
        when(qmaStrategicIndicators.HistoricalData(eq(strategic_indicator.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateIndicatorAlert(strategic_indicator, 0.33f);

        // Then
        verify(siRepository, times(1)).findByExternalIdAndProjectId(strategic_indicator.getExternalId(), project.getId());
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(strategic_indicator.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(0.33f, alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateStrategicIndicatorAlertWithCategoryUpgrade() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();

        Strategic_Indicator strategic_indicator = domainObjectsBuilder.buildStrategicIndicator(project);
        strategic_indicator.setThreshold(null); //we want it to not have a threshold for this test
        when(siRepository.findByExternalIdAndProjectId(strategic_indicator.getExternalId(), project.getId())).thenReturn(strategic_indicator);
        List<SICategory> siCategories = domainObjectsBuilder.buildSICategoryList();
        when(siCategoryRepository.findAll()).thenReturn(siCategories);

        //creating a previous evaluation that will have a lower value, so it will be in a different level and raise an alert
        DTOFactorEvaluation dtoFactorEvaluation = domainObjectsBuilder.buildDTOFactor();
        List<DTOFactorEvaluation> dtoFactorList = new ArrayList<>();
        dtoFactorList.add(dtoFactorEvaluation);

        DTOStrategicIndicatorEvaluation dtoStrategicIndicatorEvaluation = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        DTODetailedStrategicIndicatorEvaluation  previousEval = new DTODetailedStrategicIndicatorEvaluation(dtoStrategicIndicatorEvaluation.getId(), dtoStrategicIndicatorEvaluation.getName(), dtoFactorList);
        previousEval.setValue(Pair.of(0.3f, "0.3"));
        List<DTODetailedStrategicIndicatorEvaluation> previousEvals = Arrays.asList(previousEval);
        when(qmaStrategicIndicators.HistoricalData(eq(strategic_indicator.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateIndicatorAlert(strategic_indicator, 0.8f);

        // Then
        verify(siRepository, times(1)).findByExternalIdAndProjectId(strategic_indicator.getExternalId(), project.getId());
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(strategic_indicator.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(0.8f, alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateStrategicIndicatorAlertWithThresholdTrespassed() throws IOException, ProjectNotFoundException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();

        Strategic_Indicator strategic_indicator = domainObjectsBuilder.buildStrategicIndicator(project);
        float value = 0.3f; //threshold is 0.5f so the alert should be created at least for the threshold trespassed
        when(siRepository.findByExternalIdAndProjectId(strategic_indicator.getExternalId(),project.getId())).thenReturn(strategic_indicator);

        // When
        alertsController.shouldCreateIndicatorAlert(strategic_indicator, value);

        // Then
        verify(siRepository, times(1)).findByExternalIdAndProjectId(strategic_indicator.getExternalId(), project.getId());
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(strategic_indicator.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(strategic_indicator.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }
    @Test
    public void findCategoryLevel(){
        //Given
        float valueToClassify = 0.3f;
        List <Float> categoryThresholds = Arrays.asList(1.0f,0.67f,0.33f);

        //When
        int levelFound = alertsController.findCategoryLevel(valueToClassify, categoryThresholds);

        //Then
        assertEquals(levelFound, 3 );
    }
    @Test
    public void changeAlertStatusToViewed() {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Alert alert = domainObjectsBuilder.buildAlert(project);

        // When
        alertsController.changeAlertStatusToViewed(alert);

        // Then
        verify(alertRepository, times(1)).setViewedStatus(alert.getId());
    }

    @Test
    public void countNewAlerts() {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        when(alertRepository.countByProjectIdAndStatus(project.getId(), AlertStatus.NEW)).thenReturn(1);

        // When
       int newAlertsFound = alertsController.countNewAlerts(project.getId());

        // Then
        assertEquals(1, newAlertsFound);
    }
    @Test
    public void getAllAlerts() {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Alert alert = domainObjectsBuilder.buildAlert(project);
        List<Alert> alertList = new ArrayList<>();
        alertList.add(alert);
        when(alertRepository.findAll()).thenReturn(alertList);

        // When
        List<Alert> alertsFound = alertsController.getAllAlerts();

        // Then
        assertEquals(alertsFound.size(), 1);
        assertEquals(alert, alertsFound.get(0));
    }

    @Test
    public void getAllProjectAlerts() {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Alert alert = domainObjectsBuilder.buildAlert(project);
        List<Alert> alertList = new ArrayList<>();
        alertList.add(alert);
        when(alertRepository.findAllByProjectId(project.getId())).thenReturn(alertList);

        // When
        List<Alert> alertsFound = alertsController.getAllProjectAlerts(project.getId());

        // Then
        assertEquals(alertsFound.size(), 1);
        assertEquals(alert, alertsFound.get(0));
    }

    @Test
    public void getAlertById() throws AlertNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Alert alert = domainObjectsBuilder.buildAlert(project);
        when(alertRepository.findAlertById(alert.getId())).thenReturn(alert);

        // When
        Alert alertObtained = alertsController.getAlertById(alert.getId());

        // Then
        assertEquals(alert, alertObtained);
    }

    @Test(expected = AlertNotFoundException.class)
    public void getAlertByIdNotExisting () throws AlertNotFoundException {
        // Given
        long alertId = 1;
        // Throw
        Alert alertObtained = alertsController.getAlertById(alertId);
    }

}
