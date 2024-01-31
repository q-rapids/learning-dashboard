package com.upc.gessi.qrapids.app.domain.controllers;

import com.mongodb.MongoException;
import com.upc.gessi.qrapids.app.domain.adapters.Forecast;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAMetrics;
import com.upc.gessi.qrapids.app.domain.exceptions.*;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Metric.MetricRepository;
import com.upc.gessi.qrapids.app.domain.repositories.MetricCategory.MetricCategoryRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMetricEvaluation;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudent;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudentIdentity;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
public class MetricsController {

    @Autowired
    private QMAMetrics qmaMetrics;

    @Autowired
    private Forecast qmaForecast;

    @Autowired
    private MetricCategoryRepository metricCategoryRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private ProjectsController projectController;

    @Autowired
    private StudentsController studentsController;

    @Autowired
    private AlertsController alertsController;

    public Metric findMetricByExternalId (String externalId) throws MetricNotFoundException {
        Metric metric = metricRepository.findByExternalId(externalId);
        if (metric == null) {
            throw new MetricNotFoundException(externalId);
        }
        return metric;
    }

    public Metric findMetricByExternalIdAndProjectId(String externalId, Long prjId) throws MetricNotFoundException {
        Metric metric = metricRepository.findByExternalIdAndProjectId(externalId,prjId);
        if (metric == null) {
            throw new MetricNotFoundException("The metric identifier '%s' not found for project %s", externalId, prjId.toString());
        }
        return metric;
    }

    public Metric getMetricById (Long metricId) throws MetricNotFoundException {
        Optional<Metric> metricOptional = metricRepository.findById(metricId);
        if (metricOptional.isPresent()) {
            return metricOptional.get();
        } else {
            throw new MetricNotFoundException(metricId.toString());
        }
    }

    public void normalizeMetricsEvaluation(List<DTOMetricEvaluation> metrics, List<DTOStudent> students, Map<Long, String> normalizedNames) {
        metrics.forEach(metric -> {
            students.forEach(student -> {
                List<DTOStudentIdentity> studentIdentities = new ArrayList<>(student.getIdentities().values());
                metric.setName(studentsController.normalizedName(metric.getName(), studentIdentities, normalizedNames.get(student.getId())));
            });
        });
    }



    public void normalizeMetrics(List<Metric> metrics, List<DTOStudent> students, Map<Long, String> normalizedNames) {
        Map<Long, DTOStudent> studentMap = new HashMap<>();
        students.forEach(student -> {
            studentMap.put(student.getId(), student);
        });

        metrics.forEach(metric -> {
            if(metric.getStudent() != null) {
                Long studentId = metric.getStudent().getId();
                List<DTOStudentIdentity> identities = new ArrayList<>(studentMap.get(studentId).getIdentities().values());
                metric.setName(studentsController.normalizedName(metric.getName(), identities ,  normalizedNames.get(studentId)));
            }

        });
    }







    public List<Metric> getMetricsByProject (String prj) throws ProjectNotFoundException {
        Project project = projectController.findProjectByExternalId(prj);
        List<Metric> metrics = metricRepository.findByProject_IdOrderByName(project.getId());
        Map<Long,String> normalizedNames = studentsController.getNormalizedNamesByProject(project);

        List<DTOStudent> students = studentsController.getStudentsDTOFromProject(project.getId());

        normalizeMetrics(metrics, students, normalizedNames);

        return metrics;
    }

    public void importMetricsAndUpdateDatabase() throws IOException {
        List<String> projects = projectController.getAllProjectsExternalID();
        for (String prj : projects) {
            List<DTOMetricEvaluation> metrics = getAllMetricsCurrentEvaluation(prj, null);
            try {
                updateDataBaseWithNewMetrics(prj, metrics);
            } catch (ProjectNotFoundException e) {
                Logger logger = LoggerFactory.getLogger(MetricsController.class);
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void updateDataBaseWithNewMetrics (String prjExternalID, List<DTOMetricEvaluation> metrics) throws ProjectNotFoundException {
        for (DTOMetricEvaluation metric : metrics) {
            Project project = projectController.findProjectByExternalId(prjExternalID);
            Metric metricsSaved = metricRepository.findByExternalIdAndProjectId(metric.getId(),project.getId());
            if (metricsSaved == null) {
                Metric newMetric = new Metric(metric.getId(), metric.getName(),metric.getDescription(), project, "Default");
                newMetric.setStudent(null);
                metricRepository.save(newMetric);
            }
        }
    }

    public List<MetricCategory> getMetricCategories (String name) {
        List<MetricCategory> metricCategoryList = new ArrayList<>();
        Iterable<MetricCategory> metricCategoryIterable;
        if(name!=null) metricCategoryIterable = metricCategoryRepository.findAllByName(name);
        else metricCategoryIterable = metricCategoryRepository.findAll();
        metricCategoryIterable.forEach(metricCategoryList::add);
        return metricCategoryList;
    }

    public List<String> getAllNames() {
        //If name doesnt exists returns true
        Iterable<MetricCategory> categories = metricCategoryRepository.findAll();
        List<String> names = new ArrayList<String>();
        for(MetricCategory m : categories)  {
            if(!names.contains(m.getName())) names.add(m.getName());
        }
        return names;

    }

    public void deleteMetricCategory(String name) {
        Iterable<MetricCategory> metricCategoryIterable = metricCategoryRepository.findAllByName(name);
        for(MetricCategory m : metricCategoryIterable)  {
            metricCategoryRepository.deleteById(m.getId());
        }

    }

    public void updateMetricCategory(List<Map<String, String>> categories ,String name) throws CategoriesException {

        if(checkIfCategoriesHasRepeats(categories)) throw new CategoriesException(Messages.CATEGORIES_HAVE_REPEATS);
        deleteMetricCategory(name);
        newMetricCategories(categories, name);
    }

    public boolean CheckIfNameExists(String name) {
        //If name doesnt exists returns true
        return metricCategoryRepository.existsByName(name);
    }

    public boolean checkIfCategoriesHasRepeats(List<Map<String, String>> categories) {
        List<String> repeated = new ArrayList<>();
        for (Map<String, String> c : categories) {
            if (repeated.contains(c.get("type"))) {
                return true;
            }
            repeated.add(c.get("type"));
        }
        return false;
    }

    public void newMetricCategories (List<Map<String, String>> categories, String name) throws CategoriesException {

        boolean exists=CheckIfNameExists(name);
        if(exists) throw new CategoriesException(Messages.CATEGORY_ALREADY_EXISTS);

        if(checkIfCategoriesHasRepeats(categories)) throw new CategoriesException(Messages.CATEGORIES_HAVE_REPEATS);

        if (categories.size() > 0) {
            //metricCategoryRepository.deleteAll();
            for (Map<String, String> c : categories) {
                MetricCategory metricCategory = new MetricCategory();
                metricCategory.setName(name);
                metricCategory.setType(c.get("type"));
                metricCategory.setColor(c.get("color"));
                float upperThreshold = Float.parseFloat(c.get("upperThreshold"));
                metricCategory.setUpperThreshold(upperThreshold/100f);
                metricCategoryRepository.save(metricCategory);
            }
        } else {
            throw new CategoriesException(Messages.NOT_ENOUGH_CATEGORIES);
        }
    }

    public void setMetricQualityFactorRelation (List<DTOMetricEvaluation> metricList, String projectExternalId) throws IOException {
        qmaMetrics.setMetricQualityFactorRelation(metricList, projectExternalId);
    }

    public List<DTOMetricEvaluation> getAllMetricsCurrentEvaluation (String projectExternalId, String profileId) throws IOException, MongoException {
        return qmaMetrics.CurrentEvaluation(null, projectExternalId, profileId);
    }

    public DTOMetricEvaluation getSingleMetricCurrentEvaluation (String metricId, String projectExternalId) throws IOException, MongoException {
        return qmaMetrics.SingleCurrentEvaluation(metricId, projectExternalId);
    }

    public List<DTOMetricEvaluation> getMetricsForQualityFactorCurrentEvaluation (String qualityFactorId, String projectExternalId) throws IOException, MongoException {
        // it's already filtered by quality factor
        return qmaMetrics.CurrentEvaluation(qualityFactorId, projectExternalId, null);
    }

    public List<DTOMetricEvaluation> getSingleMetricHistoricalEvaluation (String metricId, String projectExternalId, String profileId, LocalDate from, LocalDate to) throws IOException, MongoException {
        return qmaMetrics.SingleHistoricalData(metricId, from, to, projectExternalId, profileId);
    }

    public List<DTOMetricEvaluation> getAllMetricsHistoricalEvaluation (String projectExternalId, String profileId, LocalDate from, LocalDate to) throws IOException, MongoException {
        return qmaMetrics.HistoricalData(null, from, to, projectExternalId, profileId);
    }

    public List<DTOMetricEvaluation> getMetricsForQualityFactorHistoricalEvaluation (String qualityFactorId, String projectExternalId, LocalDate from, LocalDate to) throws IOException, MongoException {
        // it's already filtered by quality factor
        return qmaMetrics.HistoricalData(qualityFactorId, from, to, projectExternalId, null);
    }

    public List<DTOMetricEvaluation> getMetricsPrediction (List<DTOMetricEvaluation> currentEvaluation, String projectExternalId, String technique, String freq, String horizon) throws IOException, MongoException, MetricNotFoundException, QualityFactorNotFoundException, StrategicIndicatorNotFoundException {
        List<DTOMetricEvaluation> forecast = qmaForecast.ForecastMetric(currentEvaluation, technique, freq, horizon, projectExternalId);
        int period = Integer.parseInt(horizon);
        int j = 0;
        for(int i = 0; i < forecast.size(); i += period, ++j){
            while (i < forecast.size() && forecast.get(i).getForecastingError() != null){
                ++i; ++j;
            }
            if (i >= forecast.size()) break;
            int subListEnd = Math.min(i + period, forecast.size());
            List<DTOMetricEvaluation> forecastedValues = new ArrayList<>(forecast.subList(i, subListEnd));
            alertsController.checkAlertsForMetricsPrediction(currentEvaluation.get(j), forecastedValues, projectExternalId, technique);
        }
        return forecast;
    }

    public String getMetricLabelFromValue(Float value) {
        List<MetricCategory> metricCategoryList = metricCategoryRepository.findAllByOrderByUpperThresholdAsc();
        if (value != null) {
            for (MetricCategory metricCategory : metricCategoryList) {
                if (value <= metricCategory.getUpperThreshold())
                    return metricCategory.getName();
            }
        }
        return "No Category";
    }

    public List<DTOMetricEvaluation> getAllMetricsEvaluation(String projectExternalId, String profileId) throws IOException {
        return qmaMetrics.getAllMetrics(projectExternalId, profileId);
    }

    public Metric editMetric(Long id, String threshold, String webUrl, String categoryName) throws MetricNotFoundException {
        Metric metric = getMetricById(id);
        if (!threshold.isEmpty()) // check if threshold is specified and then set it
            metric.setThreshold(Float.parseFloat(threshold));
        else
            metric.setThreshold(null);
        metric.setCategoryName(categoryName);
        metric.setWebUrl(webUrl); // set kibana url
        metricRepository.save(metric);
        return metric;
    }

    public List<MetricCategory> getMetricCategories() {
        return (List<MetricCategory>) metricCategoryRepository.findAll();
    }

    public void updateStudentMetricsByIds(List<Long> metricsIds, Student student){
        metricsIds.forEach(metricId ->{
            Optional<Metric> metricSearchResult = metricRepository.findById(metricId);
            if (metricSearchResult.isPresent()) {
                Metric metric = metricSearchResult.get();
                metric.setStudent(student);
                metricRepository.save(metric);
            }
        });
    }
}
