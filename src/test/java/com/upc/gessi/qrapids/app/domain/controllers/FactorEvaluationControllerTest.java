package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.Forecast;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAQualityFactors;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMASimulation;
import com.upc.gessi.qrapids.app.domain.exceptions.*;
import com.upc.gessi.qrapids.app.domain.models.QFCategory;
import com.upc.gessi.qrapids.app.domain.repositories.QFCategory.QFCategoryRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOFactorCategory;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOFactorEvaluation;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODetailedFactorEvaluation;
import com.upc.gessi.qrapids.app.testHelpers.DomainObjectsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FactorEvaluationControllerTest {

    private DomainObjectsBuilder domainObjectsBuilder;

    @Mock
    private QMAQualityFactors qmaQualityFactors;

    @Mock
    private AlertsController alertsController;

    @Mock
    private Forecast qmaForecast;

    @Mock
    private QMASimulation qmaSimulation;

    @Mock
    private QFCategoryRepository factorCategoryRepository;

    @Autowired
    private QFCategoryRepository factorCategoryRepositoryold;

    @Autowired
    private TestEntityManager entityManager;

    @InjectMocks
    private FactorsController factorsController;

    @Before
    public void setUp() {
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getFactorCategories() {
        // Given
        List<QFCategory> factorCategoryList = domainObjectsBuilder.buildFactorCategoryList();
        when(factorCategoryRepository.findAll()).thenReturn(factorCategoryList);

        // When
        List<QFCategory> factorCategoryListFound = factorsController.getFactorCategories(null);

        // Then
        assertEquals(factorCategoryList.size(), factorCategoryListFound.size());
        assertEquals(factorCategoryList.get(0), factorCategoryListFound.get(0));
        assertEquals(factorCategoryList.get(1), factorCategoryListFound.get(1));
        assertEquals(factorCategoryList.get(2), factorCategoryListFound.get(2));
    }

    @Test
    public void newFactorCategories() throws CategoriesException {
        // Given
        List<Map<String, String>> categories = domainObjectsBuilder.buildRawFactorCategoryList();

        // When
        factorsController.newFactorCategories(categories, "TEST");

        // Then
        //verify(factorCategoryRepository, times(1)).deleteAll("TEST");
        verify(factorCategoryRepository, times(1)).existsByName("TEST");

        ArgumentCaptor<QFCategory> factorCategoryArgumentCaptor = ArgumentCaptor.forClass(QFCategory.class);
        verify(factorCategoryRepository, times(3)).save(factorCategoryArgumentCaptor.capture());
        List<QFCategory> factorCategoryListSaved = factorCategoryArgumentCaptor.getAllValues();
        assertEquals(categories.get(0).get("type"), factorCategoryListSaved.get(0).getType());
        assertEquals(categories.get(0).get("color"), factorCategoryListSaved.get(0).getColor());
        assertEquals(Float.parseFloat(categories.get(0).get("upperThreshold")) / 100f, factorCategoryListSaved.get(0).getUpperThreshold(), 0f);
        assertEquals(categories.get(1).get("type"), factorCategoryListSaved.get(1).getType());
        assertEquals(categories.get(1).get("color"), factorCategoryListSaved.get(1).getColor());
        assertEquals(Float.parseFloat(categories.get(1).get("upperThreshold")) / 100f, factorCategoryListSaved.get(1).getUpperThreshold(), 0f);
        assertEquals(categories.get(2).get("type"), factorCategoryListSaved.get(2).getType());
        assertEquals(categories.get(2).get("color"), factorCategoryListSaved.get(2).getColor());
        assertEquals(Float.parseFloat(categories.get(2).get("upperThreshold")) / 100f, factorCategoryListSaved.get(2).getUpperThreshold(), 0f);
    }

    @Test(expected = CategoriesException.class)
    public void newFactorCategoriesNotEnough() throws CategoriesException {
        // Given
        List<Map<String, String>> categories = domainObjectsBuilder.buildRawSICategoryList();
        categories.remove(2);
        categories.remove(1);

        // Throw
        factorsController.newFactorCategories(categories, "Default");
    }

    @Test
    public void getSingleFactorEvaluation() throws IOException, QualityFactorNotFoundException {
        // Given
        DTOFactorEvaluation dtoFactorEvaluation = domainObjectsBuilder.buildDTOFactor();
        String projectExternalId = "test";
        when(qmaQualityFactors.SingleCurrentEvaluation(dtoFactorEvaluation.getId(), projectExternalId)).thenReturn(dtoFactorEvaluation);

        // When
        DTOFactorEvaluation dtoFactorEvaluationFound = factorsController.getSingleFactorEvaluation(dtoFactorEvaluation.getId(), projectExternalId);

        // Then
        assertEquals(dtoFactorEvaluation, dtoFactorEvaluationFound);
    }

    @Test
    public void getAllFactorsEvaluation() throws IOException {
        // Given
        DTOFactorEvaluation dtoFactorEvaluation = domainObjectsBuilder.buildDTOFactor();
        List<DTOFactorEvaluation> dtoFactorEvaluationList = new ArrayList<>();
        dtoFactorEvaluationList.add(dtoFactorEvaluation);
        String projectExternalId = "test";
        when(qmaQualityFactors.getAllFactors(projectExternalId, null,false)).thenReturn(dtoFactorEvaluationList);

        // When
        List<DTOFactorEvaluation> dtoFactorEvaluationListFound = factorsController.getAllFactorsEvaluation(projectExternalId, null,false);

        // Then
        assertEquals(dtoFactorEvaluationList.size(), dtoFactorEvaluationListFound.size());
        assertEquals(dtoFactorEvaluation, dtoFactorEvaluationListFound.get(0));
    }

    @Test
    public void getAllFactorsWithMetricsCurrentEvaluation() throws IOException, ProjectNotFoundException {
        // Given
        DTODetailedFactorEvaluation dtoDetailedFactorEvaluation = domainObjectsBuilder.buildDTOQualityFactor();
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationList = new ArrayList<>();
        dtoDetailedFactorEvaluationList.add(dtoDetailedFactorEvaluation);
        String projectExternalId = "test";

        String profileId = "null"; // without profile
        when(qmaQualityFactors.CurrentEvaluation(null, projectExternalId, profileId, true)).thenReturn(dtoDetailedFactorEvaluationList);

        // When
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationListFound = factorsController.getAllFactorsWithMetricsCurrentEvaluation(projectExternalId, profileId, true);
        // Then
        assertEquals(dtoDetailedFactorEvaluationList.size(), dtoDetailedFactorEvaluationListFound.size());
        assertEquals(dtoDetailedFactorEvaluation, dtoDetailedFactorEvaluationListFound.get(0));
    }

    @Test
    public void getFactorsWithMetricsForOneStrategicIndicatorCurrentEvaluation() throws IOException, ProjectNotFoundException {
        // Given
        DTODetailedFactorEvaluation dtoDetailedFactorEvaluation = domainObjectsBuilder.buildDTOQualityFactor();
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationList = new ArrayList<>();
        dtoDetailedFactorEvaluationList.add(dtoDetailedFactorEvaluation);
        String strategicIndicatorId = "processperformance";
        String projectExternalId = "test";

        when(qmaQualityFactors.CurrentEvaluation(strategicIndicatorId, projectExternalId, null, true)).thenReturn(dtoDetailedFactorEvaluationList);
        // When
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationListFound = factorsController.getFactorsWithMetricsForOneStrategicIndicatorCurrentEvaluation(strategicIndicatorId, projectExternalId);

        // Then
        assertEquals(dtoDetailedFactorEvaluationList.size(), dtoDetailedFactorEvaluationListFound.size());
        assertEquals(dtoDetailedFactorEvaluation, dtoDetailedFactorEvaluationListFound.get(0));
    }

    @Test
    public void getAllFactorsWithMetricsHistoricalEvaluation() throws IOException, ProjectNotFoundException {
        // Given
        DTODetailedFactorEvaluation dtoDetailedFactorEvaluation = domainObjectsBuilder.buildDTOQualityFactor();
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationList = new ArrayList<>();
        dtoDetailedFactorEvaluationList.add(dtoDetailedFactorEvaluation);
        String projectExternalId = "test";

        String profileId = "null"; // without profile
        LocalDate from = dtoDetailedFactorEvaluation.getMetrics().get(0).getDate().minusDays(7);
        LocalDate to = dtoDetailedFactorEvaluation.getMetrics().get(0).getDate();
        when(qmaQualityFactors.HistoricalData(null, from, to, projectExternalId, profileId)).thenReturn(dtoDetailedFactorEvaluationList);

        // When
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationListFound = factorsController.getAllFactorsWithMetricsHistoricalEvaluation(projectExternalId, profileId, from, to);

        // Then
        assertEquals(dtoDetailedFactorEvaluationList.size(), dtoDetailedFactorEvaluationListFound.size());
        assertEquals(dtoDetailedFactorEvaluation, dtoDetailedFactorEvaluationListFound.get(0));
    }

    @Test
    public void getFactorsWithMetricsForOneStrategicIndicatorHistoricalEvaluation() throws IOException, ProjectNotFoundException {
        // Given
        DTODetailedFactorEvaluation dtoDetailedFactorEvaluation = domainObjectsBuilder.buildDTOQualityFactor();
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationList = new ArrayList<>();
        dtoDetailedFactorEvaluationList.add(dtoDetailedFactorEvaluation);
        String strategicIndicatorId = "processperformance";
        String projectExternalId = "test";
        LocalDate from =  dtoDetailedFactorEvaluation.getMetrics().get(0).getDate().minusDays(7);
        LocalDate to =  dtoDetailedFactorEvaluation.getMetrics().get(0).getDate();
        when(qmaQualityFactors.HistoricalData(strategicIndicatorId, from, to, projectExternalId, null)).thenReturn(dtoDetailedFactorEvaluationList);

        // When
        List<DTODetailedFactorEvaluation> dtoDetailedFactorEvaluationListFound = factorsController.getFactorsWithMetricsForOneStrategicIndicatorHistoricalEvaluation(strategicIndicatorId, projectExternalId, from, to);

        // Then
        assertEquals(dtoDetailedFactorEvaluationList.size(), dtoDetailedFactorEvaluationListFound.size());
        assertEquals(dtoDetailedFactorEvaluation, dtoDetailedFactorEvaluationListFound.get(0));
    }

    @Test
    public void getAllFactorsWithMetricsPrediction() throws IOException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        // Given
        DTODetailedFactorEvaluation dtoDetailedFactorEvaluationCurrentEvaluation = domainObjectsBuilder.buildDTOQualityFactor();
        List<DTODetailedFactorEvaluation> currentEvaluation = new ArrayList<>();
        currentEvaluation.add(dtoDetailedFactorEvaluationCurrentEvaluation);

        DTODetailedFactorEvaluation dtoDetailedFactorEvaluationPrediction = domainObjectsBuilder.buildDTOQualityFactorForPrediction();
        List<DTODetailedFactorEvaluation> prediction = new ArrayList<>();
        prediction.add(dtoDetailedFactorEvaluationPrediction);
        String technique = "PROPHET";
        String freq = "7";
        String horizon = "7";
        String projectExternalId = "test";
        when(qmaForecast.ForecastDetailedFactor(currentEvaluation, technique, freq, horizon, projectExternalId)).thenReturn(prediction);

        // When
        List<DTODetailedFactorEvaluation> predictionFound = factorsController.getFactorsWithMetricsPrediction(currentEvaluation, technique, freq, horizon, projectExternalId);

        // Then
        assertEquals(prediction.size(), predictionFound.size());
        assertEquals(dtoDetailedFactorEvaluationPrediction, predictionFound.get(0));
    }

    @Test
    public void simulate() throws IOException {
        // Given
        DTOFactorEvaluation dtoFactorEvaluation = domainObjectsBuilder.buildDTOFactor();
        List<DTOFactorEvaluation> dtoFactorEvaluationList = new ArrayList<>();
        dtoFactorEvaluationList.add(dtoFactorEvaluation);

        String projectExternalId = "test";
        String metricId = "fasttests";
        Float metricValue = 0.7f;
        LocalDate date = LocalDate.parse("2019-07-07");
        Map<String, String> metric = new HashMap<>();
        metric.put("id", metricId);
        metric.put("value", metricValue.toString());
        List<Map<String, String>> metricList = new ArrayList<>();
        metricList.add(metric);

        Map<String, Float> metricsMap = new HashMap<>();
        metricsMap.put(metricId, metricValue);

        when(qmaSimulation.simulateQualityFactors(metricsMap, projectExternalId,null, date)).thenReturn(dtoFactorEvaluationList);

        // When
        List<DTOFactorEvaluation> factorsSimulationList = factorsController.simulate(metricsMap, projectExternalId, null, date);

        // Then
        assertEquals(dtoFactorEvaluationList.size(), factorsSimulationList.size());
        assertEquals(dtoFactorEvaluation, factorsSimulationList.get(0));
    }
    /*

    @Test
    public void getFactorLabelFromValueGood() {
        // Given
        List<QFCategory> qfCategoryList = domainObjectsBuilder.buildFactorCategoryList();
        Collections.reverse(qfCategoryList);
        when(factorCategoryRepository.findAllByOrderByUpperThresholdAsc()).thenReturn(qfCategoryList);
        float value = 0.8f;

        // When
        String label = factorsController.getFactorLabelFromValue(value);

        // Then
        String expectedLabel = "Good";
        assertEquals(expectedLabel, label);
    }

    @Test
    public void getFactorLabelFromValueNeutral() {
        // Given
        List<QFCategory> qfCategoryList = domainObjectsBuilder.buildFactorCategoryList();
        Collections.reverse(qfCategoryList);
        when(factorCategoryRepository.findAllByOrderByUpperThresholdAsc()).thenReturn(qfCategoryList);
        float value = 0.5f;

        // When
        String label = factorsController.getFactorLabelFromValue(value);

        // Then
        String expectedLabel = "Neutral";
        assertEquals(expectedLabel, label);
    }

    @Test
    public void getFactorLabelFromValueBad() {
        // Given
        List<QFCategory> qfCategoryList = domainObjectsBuilder.buildFactorCategoryList();
        Collections.reverse(qfCategoryList);
        when(factorCategoryRepository.findAllByOrderByUpperThresholdAsc()).thenReturn(qfCategoryList);
        float value = 0.2f;

        // When
        String label = factorsController.getFactorLabelFromValue(value);

        // Then
        String expectedLabel = "Bad";
        assertEquals(expectedLabel, label);
    }
*/
    @Test
    public void getFactorLabelFromValueAndName() {
        // Given
        List<QFCategory> qfCategoryList = domainObjectsBuilder.buildFactorCategoryList();
        Collections.reverse(qfCategoryList);
        when(factorCategoryRepository.findAllByOrderByUpperThresholdAsc()).thenReturn(qfCategoryList);

        // When
        String label_up_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0.7f);
        String label_low_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0.10f);
        String label_low2_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0.15f);
        String label_low3_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0f);
        String label_good_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0.37f);
        String label_up2_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 0.8f);
        String label_high2_6mem = factorsController.getFactorLabelFromNameAndValue("6 members contribution", 1f);

        // Then
        assertEquals("Up", label_up_6mem);
        assertEquals("Good enough", label_good_6mem);
        assertEquals("Up", label_up2_6mem);
        assertEquals("High", label_high2_6mem);
        assertEquals("Low", label_low_6mem);
        assertEquals("Low", label_low2_6mem);
        assertEquals("Low", label_low3_6mem);

    }

    @Test
    public void getCategoryFromRationale (){
        String rationale_example = "metrics: { commits_anonymous (value: 0.13736264, no weighted); }, formula: average, value: 0.13736264, category: Default";
        String cat_def = factorsController.getCategoryFromRationale(rationale_example);
        rationale_example = "metrics: { commits_anonymous (value: 0.6264, no weighted); }, formula: average, value: 0.6264, category: 6 members contribution";
        String cat_6m = factorsController.getCategoryFromRationale(rationale_example);
        rationale_example = "metrics: { commits_anonymous (value: 0.482, no weighted); }, formula: average, value: 0.482, category: Reversed default";
        String cat_rev = factorsController.getCategoryFromRationale(rationale_example);

        assertEquals("Default", cat_def);
        assertEquals("6 members contribution", cat_6m);
        assertEquals("Reversed default", cat_rev);
    }


}