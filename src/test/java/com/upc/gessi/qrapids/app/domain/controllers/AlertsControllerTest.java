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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.springframework.data.util.Pair;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AlertsControllerTest {
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
    private ProjectRepository projectRepository;
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
        alertsController.createAlert(value, threshold, type, project, affectedId, affectedType, null, null);

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
        assertNull(alertCreated.getPredictionDate());
        assertNull(alertCreated.getPredictionTechnique());
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
        metric.setCategoryName(null); //we don't want it to have a category for this test
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
    public void shouldNotCreateMetricAlertBecauseTodayExactAlertHasBeenCreated() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
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
        Alert alert = domainObjectsBuilder.buildAlert(project);
        alert.setValue(value);
        alert.setThreshold(null);
        when(alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                eq(projectId), eq(metric.getExternalId()), eq("metric"),
                eq(AlertType.CATEGORY_DOWNGRADE), any(Date.class), any(Date.class))).thenReturn(alert);

        // When
        alertsController.shouldCreateMetricAlert(metricEval, value, projectId);

        // Then
        verify(metricRepository, times(1)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(0)).save(alertArgumentCaptor.capture());
    }

    @Test
    public void shouldCreateMetricAlertBecauseTodayExactAlertHasBeenCreatedButDifferent() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
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
        Alert alert = domainObjectsBuilder.buildAlert(project);
        alert.setValue(0.5f);
        alert.setThreshold(null);
        when(alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateGreaterThanEqualAndDateLessThan(
                eq(projectId), eq(metric.getExternalId()), eq("metric"),
                eq(AlertType.CATEGORY_DOWNGRADE), any(Date.class), any(Date.class))).thenReturn(alert);

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
        assertEquals(alertSaved.getValue(), value, 0.0f);
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
        factor.setCategoryName(null); //we don't want it to have a category for this test
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
    public void shouldCreateThresholdNonTreatedAlert() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation currentMetricEval = domainObjectsBuilder.buildDTOMetric();
        float value = 0.3f; //threshold is 0.5f so value < threshold

        Metric currentMetric = domainObjectsBuilder.buildMetric(project);
        currentMetric.setCategoryName(null); //we don't want it to have a category for this test
        currentMetric.setExternalId(currentMetricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(currentMetricEval.getId(), projectId)).thenReturn(currentMetric);

        //create a previous alert for a trespassed threshold
        Alert previousAlert = domainObjectsBuilder.buildAlert(project);
        Date today = new Date();
        Date alertDate = new Date(today.getTime()-86400000*8);
        previousAlert.setDate(alertDate);
        when(alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant()))
                .thenReturn(previousAlert);

        //create previous evaluations
        DTOMetricEvaluation previousEval = domainObjectsBuilder.buildDTOMetric();
        previousEval.setValue(0.2f);
        DTOMetricEvaluation previousEvalTwo = domainObjectsBuilder.buildDTOMetric();
        previousEvalTwo.setValue(0.1f);
        DTOMetricEvaluation previousEvalThree = domainObjectsBuilder.buildDTOMetric();
        previousEvalThree.setValue(0.35f);
        List<DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval, previousEvalTwo, previousEvalThree);
        when(qmaMetrics.SingleHistoricalData(eq(currentMetric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateMetricAlert(currentMetricEval, value, projectId);

        // Then
        verify(alertRepository, times (1)).findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant());
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(currentMetricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(currentMetric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(currentMetric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.ALERT_NOT_TREATED, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(currentMetric.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldNotCreateThresholdNonTreatedAlertBecauseOfDates() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation currentMetricEval = domainObjectsBuilder.buildDTOMetric();
        float value = 0.3f; //threshold is 0.5f so value < threshold

        Metric currentMetric = domainObjectsBuilder.buildMetric(project);
        currentMetric.setCategoryName(null); //we don't want it to have a category for this test
        currentMetric.setExternalId(currentMetricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(currentMetricEval.getId(), projectId)).thenReturn(currentMetric);

        //create a previous alert for a trespassed threshold
        Alert previousAlert = domainObjectsBuilder.buildAlert(project);
        Date today = new Date();
        Date alertDate = new Date(today.getTime()-86400000);
        previousAlert.setDate(alertDate);
        when(alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant()))
                .thenReturn(previousAlert);

        //create previous evaluations
        DTOMetricEvaluation previousEval = domainObjectsBuilder.buildDTOMetric();
        previousEval.setValue(0.2f);
        DTOMetricEvaluation previousEvalTwo = domainObjectsBuilder.buildDTOMetric();
        previousEvalTwo.setValue(0.1f);
        DTOMetricEvaluation previousEvalThree = domainObjectsBuilder.buildDTOMetric();
        previousEvalThree.setValue(0.25f);
        List<DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval, previousEvalTwo, previousEvalThree);
        when(qmaMetrics.SingleHistoricalData(eq(currentMetric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateMetricAlert(currentMetricEval, value, projectId);

        // Then
        verify(alertRepository, times (1)).findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant());
        verify(metricRepository, times(1)).findByExternalIdAndProjectId(currentMetricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(currentMetric.getCategoryName());
    }

    @Test
    public void shouldNotCreateThresholdNonTreatedAlertBecauseOfImprovement() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();

        DTOMetricEvaluation currentMetricEval = domainObjectsBuilder.buildDTOMetric();
        float value = 0.3f; //threshold is 0.5f so value < threshold

        Metric currentMetric = domainObjectsBuilder.buildMetric(project);
        currentMetric.setCategoryName(null); //we don't want it to have a category for this test
        currentMetric.setExternalId(currentMetricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(currentMetricEval.getId(), projectId)).thenReturn(currentMetric);

        //create a previous alert for a trespassed threshold
        Alert previousAlert = domainObjectsBuilder.buildAlert(project);
        Date today = new Date();
        Date alertDate = new Date(today.getTime()-86400000);
        previousAlert.setDate(alertDate);
        when(alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant()))
                .thenReturn(previousAlert);

        //create previous evaluations
        DTOMetricEvaluation previousEval = domainObjectsBuilder.buildDTOMetric();
        previousEval.setValue(0.2f);
        DTOMetricEvaluation previousEvalTwo = domainObjectsBuilder.buildDTOMetric();
        previousEvalTwo.setValue(0.1f);
        DTOMetricEvaluation previousEvalThree = domainObjectsBuilder.buildDTOMetric();
        previousEvalThree.setValue(0.55f);
        List<DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval, previousEvalTwo, previousEvalThree);
        when(qmaMetrics.SingleHistoricalData(eq(currentMetric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        // When
        alertsController.shouldCreateMetricAlert(currentMetricEval, value, projectId);

        // Then
        verify(alertRepository, times (1)).findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,currentMetric.getExternalId(),"metric",AlertType.TRESPASSED_THRESHOLD, alertsController.getTodayStartOfDayInstant());
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(currentMetricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(currentMetric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(currentMetric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(value, alertSaved.getValue(), 0f);
        assertEquals(currentMetric.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void IsNotATrespassedThresholdNotTreated(){
        //Given
        float elementThreshold = 0.3f;
        List<Float> evalsAfterAlert = Arrays.asList(0.2f,0.29f,0.4f,0.3f,0.01f,0.5f);

        //When
        boolean isNonTreated= alertsController.isATrespassedThresholdNotTreated(elementThreshold,evalsAfterAlert);

        //Then
        assertFalse(isNonTreated);

    }

    @Test
    public void shouldCreateCategoryAlertNotTreated() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
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

        //creating a previous evaluation that will have the same value, so it will be in the same level and raise a check on non treated alerts
        DTOMetricEvaluation previousEval= domainObjectsBuilder.buildDTOMetric();
        List <DTOMetricEvaluation> previousEvals = Arrays.asList(previousEval);

        when(qmaMetrics.SingleHistoricalData(eq(metric.getExternalId()), any(), any(), eq(project.getExternalId()), any())).thenReturn(previousEvals);

        //creating previous alerts being the last upgrade alert older than the last downgrade alert (so its a non treated alert as it hasn't improved)
        Alert downgradeAlert = domainObjectsBuilder.buildAlert(project);
        downgradeAlert.setType(AlertType.CATEGORY_DOWNGRADE);

        Alert upgradeAlert = domainObjectsBuilder.buildAlert(project);
        upgradeAlert.setType(AlertType.CATEGORY_UPGRADE);

        Date today = new Date();
        Date downgradeAlertDate = new Date(today.getTime()-86400000*8);
        Date upgradeAlertDate = new Date(downgradeAlertDate.getTime()-86400000);
        upgradeAlert.setDate(upgradeAlertDate);
        downgradeAlert.setDate(downgradeAlertDate);

        when(alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_UPGRADE, alertsController.getTodayStartOfDayInstant()))
                .thenReturn(upgradeAlert);
        when(alertRepository.findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                metric.getProject().getId(), metric.getExternalId(), "metric", AlertType.CATEGORY_DOWNGRADE, alertsController.getTodayStartOfDayInstant()))
                .thenReturn(downgradeAlert);


        // When
        alertsController.shouldCreateMetricAlert(metricEval, metricEval.getValue(), projectId);

        // Then
        verify(alertRepository, times (1)).findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,metric.getExternalId(),"metric",AlertType.CATEGORY_DOWNGRADE, alertsController.getTodayStartOfDayInstant());
        verify(alertRepository, times (1)).findTopByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndDateLessThanOrderByIdDesc(
                projectId,metric.getExternalId(),"metric",AlertType.CATEGORY_UPGRADE, alertsController.getTodayStartOfDayInstant());
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a non-treated alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.ALERT_NOT_TREATED, alertSaved.getType());
        assertEquals(metricEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void IsATrespassedThresholdNotTreated(){
        //Given
        float elementThreshold = 0.3f;
        List<Float> evalsAfterAlert = Arrays.asList(0.2f,0.29f,0.01f,0.1f);

        //When
        boolean isNonTreated= alertsController.isATrespassedThresholdNotTreated(elementThreshold,evalsAfterAlert);

        //Then
        assertTrue(isNonTreated);
    }

    @Test
    public void shouldCreateMetricPredictionAlertWithCategoryDowngrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        //current eval
        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);
        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //predicted evals (adding more than one that has value < threshold to check that only one alert should checked and then stop
        DTOMetricEvaluation metricPredictedEval = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.05f);
        DTOMetricEvaluation metricPredictedEvalTwo = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.21f);
        List<DTOMetricEvaluation> forecast = Arrays.asList(metricPredictedEval, metricPredictedEvalTwo);

        // When
        alertsController.checkAlertsForMetricsPrediction(metricEval, forecast, project.getExternalId(), "PROPHET");

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(metricPredictedEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(metricPredictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateMetricPredictionAlertWithCategoryUpgrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        //current eval
        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        metricEval.setValue(0.30f);
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);
        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //predicted evals (adding more than one that has value < threshold to check that only one alert should checked and then stop
        DTOMetricEvaluation metricPredictedEval = domainObjectsBuilder.buildDTOMetric();
        DTOMetricEvaluation metricPredictedEvalTwo = domainObjectsBuilder.buildDTOMetric();
        List<DTOMetricEvaluation> forecast = Arrays.asList(metricPredictedEval, metricPredictedEvalTwo);

        // When
        alertsController.checkAlertsForMetricsPrediction(metricEval, forecast, project.getExternalId(), "PROPHET");

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(metricPredictedEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(metricPredictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateMetricPredictionAlertWithThresholdTrespassed() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        //current eval (must be a value over the threshold)
        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        metricEval.setValue(0.5f);

        //metric
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(0.33f);
        metric.setExternalId(metricEval.getId());
        metric.setCategoryName(null); //we don't want it to have a category for this test
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);
        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //predicted evals (adding more than one that has value < threshold to check that only one alert should checked and then stop
        DTOMetricEvaluation metricPredictedEval = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.10f);
        DTOMetricEvaluation metricPredictedEvalTwo = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEvalTwo.setValue(0.23f);
        List<DTOMetricEvaluation> forecast = Arrays.asList(metricPredictedEval, metricPredictedEvalTwo);

        // When
        alertsController.checkAlertsForMetricsPrediction(metricEval, forecast, project.getExternalId(), "PROPHET");

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(metric.getThreshold(), alertSaved.getThreshold(), 0f);
        assertEquals(metricPredictedEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(metricPredictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
    }

    @Test
    public void shouldCreateFactorPredictionAlertWithCategoryDowngrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Factor factor = domainObjectsBuilder.buildFactor(project);
        factor.setThreshold(null); //we want it to not have a threshold for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);

        List<QFCategory> factorCategories = domainObjectsBuilder.buildFactorCategoryList();
        when(QFCategoryRepository.findAllByName(factor.getCategoryName())).thenReturn(factorCategories);

        //creating the current eval that will have a higher value, so it will be in a different level thant the predicted and raise an alert
        DTODetailedFactorEvaluation currentEval = domainObjectsBuilder.buildDTOQualityFactor();
        currentEval.setValue(Pair.of(0.8f, "0.8"));

        //predicted evals
        DTODetailedFactorEvaluation predictedEval = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEval.setValue(Pair.of(0.4f, "0.4"));
        predictedEval.setDate(LocalDate.now());
        DTODetailedFactorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEvalTwo.setValue(Pair.of(0.2f, "0.2"));
        predictedEvalTwo.setDate(LocalDate.now());
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));


        // When
        alertsController.checkAlertsForFactorsPrediction(currentEval.getValue().getFirst(), factor.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(factorRepository,times(2)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldCreateFactorPredictionAlertWithCategoryUpgrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Factor factor = domainObjectsBuilder.buildFactor(project);
        factor.setThreshold(null); //we want it to not have a threshold for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);

        List<QFCategory> factorCategories = domainObjectsBuilder.buildFactorCategoryList();
        when(QFCategoryRepository.findAllByName(factor.getCategoryName())).thenReturn(factorCategories);

        //creating the current eval that will have a higher value, so it will be in a different level thant the predicted and raise an alert
        DTODetailedFactorEvaluation currentEval = domainObjectsBuilder.buildDTOQualityFactor();
        currentEval.setValue(Pair.of(0.3f, "0.3"));

        //predicted evals
        DTODetailedFactorEvaluation predictedEval = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEval.setValue(Pair.of(0.4f, "0.4"));
        predictedEval.setDate(LocalDate.now());
        DTODetailedFactorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEvalTwo.setValue(Pair.of(0.8f, "0.8"));
        predictedEvalTwo.setDate(LocalDate.now());
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));


        // When
        alertsController.checkAlertsForFactorsPrediction(currentEval.getValue().getFirst(), factor.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(factorRepository,times(2)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category upgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldCreateFactorPredictionAlertWithThresholdTrespassed() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Factor factor = domainObjectsBuilder.buildFactor(project);
        factor.setThreshold(0.33f);
        factor.setCategoryName(null); //we don't want it to have a category for this test
        when(factorRepository.findByExternalIdAndProjectId(factor.getExternalId(),projectId)).thenReturn(factor);

        //creating the current eval that will have a higher value, so it will be in a different level thant the predicted and raise an alert
        DTODetailedFactorEvaluation currentEval = domainObjectsBuilder.buildDTOQualityFactor();
        currentEval.setValue(Pair.of(0.40f, "0.40"));

        //predicted evals
        DTODetailedFactorEvaluation predictedEval = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEval.setValue(Pair.of(0.2f, "0.2"));
        predictedEval.setDate(LocalDate.now());
        DTODetailedFactorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOQualityFactor();
        predictedEvalTwo.setValue(Pair.of(0.05f, "0.05"));
        predictedEvalTwo.setDate(LocalDate.now());
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));

        // When
        alertsController.checkAlertsForFactorsPrediction(currentEval.getValue().getFirst(), factor.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(factorRepository,times(2)).findByExternalIdAndProjectId(factor.getExternalId(),projectId);
        verify(QFCategoryRepository, times(1)).findAllByName(factor.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a threshold trespassed alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(factor.getExternalId(), alertSaved.getAffectedId());
        assertEquals("factor", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldCreateIndicatorPredictionAlertWithCategoryDowngrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Strategic_Indicator si = domainObjectsBuilder.buildStrategicIndicator(project);
        si.setThreshold(null); //we want it to not have a threshold for this test
        when(siRepository.findByExternalIdAndProjectId(si.getExternalId(),projectId)).thenReturn(si);

        List<SICategory> siCategories = domainObjectsBuilder.buildSICategoryList();
        when(siCategoryRepository.findAll()).thenReturn(siCategories);

        //creating current eval
        DTOStrategicIndicatorEvaluation currentEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        currentEval.setValue(Pair.of(0.8f, "0.8"));

        //predicted evals
        DTOStrategicIndicatorEvaluation predictedEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.4f, "0.4"));
        DTOStrategicIndicatorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.3f, "0.3"));
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));


        // When
        alertsController.checkAlertsForIndicatorsPrediction(currentEval.getValue().getFirst(), si.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(siRepository,times(2)).findByExternalIdAndProjectId(si.getExternalId(),projectId);
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category downgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(si.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldCreateIndicatorPredictionAlertWithCategoryUpgrade() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Strategic_Indicator si = domainObjectsBuilder.buildStrategicIndicator(project);
        si.setThreshold(null); //we want it to not have a threshold for this test
        when(siRepository.findByExternalIdAndProjectId(si.getExternalId(),projectId)).thenReturn(si);

        List<SICategory> siCategories = domainObjectsBuilder.buildSICategoryList();
        when(siCategoryRepository.findAll()).thenReturn(siCategories);

        //creating current eval
        DTOStrategicIndicatorEvaluation currentEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        currentEval.setValue(Pair.of(0.6f, "0.6"));

        //predicted evals
        DTOStrategicIndicatorEvaluation predictedEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.8f, "0.8"));
        DTOStrategicIndicatorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.9f, "0.9"));
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));


        // When
        alertsController.checkAlertsForIndicatorsPrediction(currentEval.getValue().getFirst(), si.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(siRepository,times(2)).findByExternalIdAndProjectId(si.getExternalId(),projectId);
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category upgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(si.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_UPGRADE, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldCreateIndicatorPredictionAlertWithThresholdTrespassed() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        Strategic_Indicator si = domainObjectsBuilder.buildStrategicIndicator(project);
        si.setThreshold(0.55f);
        when(siRepository.findByExternalIdAndProjectId(si.getExternalId(),projectId)).thenReturn(si);

        //creating current eval
        DTOStrategicIndicatorEvaluation currentEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        currentEval.setValue(Pair.of(0.6f, "0.6"));

        //predicted evals
        DTOStrategicIndicatorEvaluation predictedEval = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.33f, "0.33"));
        DTOStrategicIndicatorEvaluation predictedEvalTwo = domainObjectsBuilder.buildDTOStrategicIndicatorEvaluation();
        predictedEval.setValue(Pair.of(0.23f, "0.23"));
        List<Float> values = Arrays.asList(predictedEval.getValue().getFirst(), predictedEvalTwo.getValue().getFirst());
        List<Date> dates = Arrays.asList(java.sql.Date.valueOf(predictedEval.getDate()), java.sql.Date.valueOf(predictedEvalTwo.getDate()));


        // When
        alertsController.checkAlertsForIndicatorsPrediction(currentEval.getValue().getFirst(), si.getExternalId(), values, dates, project.getExternalId(), "PROPHET");

        // Then
        verify(siRepository,times(2)).findByExternalIdAndProjectId(si.getExternalId(),projectId);
        verify(siCategoryRepository, times(1)).findAll();
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        //Checking that a category upgrade alert has been created
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(si.getExternalId(), alertSaved.getAffectedId());
        assertEquals("indicator", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_TRESPASSED_THRESHOLD, alertSaved.getType());
        assertEquals(predictedEval.getValue().getFirst(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(predictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals(AlertStatus.NEW, alertSaved.getStatus());
        assertEquals(project, alertSaved.getProject());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
    }

    @Test
    public void shouldNotCreateMetricPredictionAlertBecauseTodayExactAlertHasBeenCreated() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        //current eval
        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);
        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //predicted evals
        DTOMetricEvaluation metricPredictedEval = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.5f);
        DTOMetricEvaluation metricPredictedEvalTwo = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.21f);
        List<DTOMetricEvaluation> forecast = Arrays.asList(metricPredictedEval, metricPredictedEvalTwo);

        //create the "alreadyCreated" alert for this prediction
        Alert alert = domainObjectsBuilder.buildAlert(project);
        alert.setDate(new Date());
        alert.setPredictionDate(java.sql.Date.valueOf(metricPredictedEval.getDate()));
        alert.setValue(metricPredictedEval.getValue());
        alert.setPredictionTechnique("PROPHET");
        alert.setAffectedType("metric");
        alert.setAffectedId(metricEval.getId());
        alert.setThreshold(metric.getThreshold());
        alert.setProject(project);
        alert.setType(AlertType.PREDICTED_CATEGORY_DOWNGRADE);
        alert.setStatus(AlertStatus.NEW);
        LocalDate todayDate= LocalDate.now();
        LocalDateTime todayStart = todayDate.atStartOfDay();
        Date startDate = Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());
        when(alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(eq(projectId), eq(alert.getAffectedId()), eq("metric"), eq(alert.getType()), eq("PROPHET"), eq(alert.getPredictionDate()), eq(startDate), any())).thenReturn(alert);

        // When
        alertsController.checkAlertsForMetricsPrediction(metricEval, forecast, project.getExternalId(), "PROPHET");

        // Then
        verify(metricRepository, times(1)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(0)).save(alertArgumentCaptor.capture());
    }

    @Test
    public void shouldCreateMetricPredictionAlertBecauseTodayExactAlertHasBeenCreatedButItIsDifferent() throws MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        Long projectId = project.getId();
        when(projectRepository.findByExternalId(project.getExternalId())).thenReturn(project);

        //current eval
        DTOMetricEvaluation metricEval = domainObjectsBuilder.buildDTOMetric();
        Metric metric = domainObjectsBuilder.buildMetric(project);
        metric.setThreshold(null); //we want it to not have a threshold for this test
        metric.setExternalId(metricEval.getId());
        when(metricRepository.findByExternalIdAndProjectId(metricEval.getId(), projectId)).thenReturn(metric);
        List<MetricCategory> metricCategories = domainObjectsBuilder.buildMetricCategoryList();
        when(metricCategoryRepository.findAllByName(metric.getCategoryName())).thenReturn(metricCategories);

        //predicted evals
        DTOMetricEvaluation metricPredictedEval = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.5f);
        DTOMetricEvaluation metricPredictedEvalTwo = domainObjectsBuilder.buildDTOMetric();
        metricPredictedEval.setValue(0.21f);
        List<DTOMetricEvaluation> forecast = Arrays.asList(metricPredictedEval, metricPredictedEvalTwo);

        //create the "alreadyCreated" alert for this prediction
        Alert alert = domainObjectsBuilder.buildAlert(project);
        alert.setDate(new Date());
        alert.setPredictionDate(java.sql.Date.valueOf(metricPredictedEval.getDate()));
        alert.setValue(metricPredictedEval.getValue());
        alert.setPredictionTechnique("PROPHET");
        alert.setAffectedType("metric");
        alert.setAffectedId(metricEval.getId());
        alert.setThreshold(metric.getThreshold());
        alert.setProject(project);
        alert.setType(AlertType.PREDICTED_CATEGORY_DOWNGRADE);
        alert.setStatus(AlertStatus.NEW);
        LocalDate todayDate= LocalDate.now();
        LocalDateTime todayStart = todayDate.atStartOfDay();
        Date startDate = Date.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());
        when(alertRepository.findAlertByProjectIdAndAffectedIdAndAffectedTypeAndTypeAndPredictionTechniqueAndPredictionDateAndDateGreaterThanEqualAndDateLessThan(eq(projectId), eq(alert.getAffectedId()), eq("metric"), eq(alert.getType()), eq("PROPHET"), eq(alert.getPredictionDate()), eq(startDate), any())).thenReturn(alert);

        // When
        metricPredictedEval.setValue(0.55f);
        alertsController.checkAlertsForMetricsPrediction(metricEval, forecast, project.getExternalId(), "PROPHET");

        // Then
        verify(metricRepository, times(2)).findByExternalIdAndProjectId(metricEval.getId(), projectId);
        verify(metricCategoryRepository, times(1)).findAllByName(metric.getCategoryName());
        ArgumentCaptor<Alert> alertArgumentCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, times(1)).save(alertArgumentCaptor.capture());
        Alert alertSaved = alertArgumentCaptor.getValue();
        assertEquals(metric.getExternalId(), alertSaved.getAffectedId());
        assertEquals("metric", alertSaved.getAffectedType());
        assertEquals(AlertType.PREDICTED_CATEGORY_DOWNGRADE, alertSaved.getType());
        assertEquals(metricPredictedEval.getValue(), alertSaved.getValue(), 0f);
        assertEquals(java.sql.Date.valueOf(metricPredictedEval.getDate()), alertSaved.getPredictionDate());
        assertEquals("PROPHET", alertSaved.getPredictionTechnique());
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
    public void getAllProjectAlertsWithoutProfile() {
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
        alertsController.getAlertById(alertId);
    }

}
