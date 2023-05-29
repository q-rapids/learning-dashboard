package com.upc.gessi.qrapids.app.presentation.rest.services;


import com.upc.gessi.qrapids.app.domain.controllers.MetricsController;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.exceptions.MetricNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.elasticsearch.ElasticsearchStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
public class Metrics {

    @Autowired
    private MetricsController metricsController;

    @Autowired
    private StudentsController studentsController;

    private Logger logger = LoggerFactory.getLogger(Metrics.class);

    @GetMapping("/api/metrics/import")
    @ResponseStatus(HttpStatus.OK)
    public void importMetrics() {
        try {
            metricsController.importMetricsAndUpdateDatabase();
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CATEGORIES_DO_NOT_MATCH);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on ElasticSearch connection");
        }
    }

    @GetMapping("/api/metrics/list")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getList() {
        return metricsController.getAllNames();
    }

    @PutMapping("/api/metrics/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void editMetric(@PathVariable Long id, HttpServletRequest request) {
        try {
            String threshold = request.getParameter("threshold");
            String webUrl = request.getParameter("url");
            String categoryName = request.getParameter("categoryName");
            metricsController.editMetric(id,threshold,webUrl,categoryName); // at the moment is only possible change threshold
        } catch (MetricNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }


    @DeleteMapping("/api/metrics/students/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMetricStudent(HttpServletRequest request,@PathVariable Long id) {

        studentsController.deleteStudents(id);
    }

    @GetMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricCategory> getMetricCategories ( @RequestParam(value = "name", required = false) String name) {
        Iterable<MetricCategory> metricCategoryList = metricsController.getMetricCategories(name);
        List<DTOMetricCategory> dtoMetricCategoryList = new ArrayList<>();
        for (MetricCategory metricCategory : metricCategoryList) {
            dtoMetricCategoryList.add(new DTOMetricCategory(metricCategory.getId(), metricCategory.getName(), metricCategory.getColor(), metricCategory.getUpperThreshold(), metricCategory.getType()));
        }
        return dtoMetricCategoryList;
    }


    @PostMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public void newMetricsCategories (@RequestBody List<Map<String, String>> categories, @RequestParam(value = "name", required = false) String name) {
        try {
            if(categories.size()<3) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.NOT_ENOUGH_CATEGORIES);
            else metricsController.newMetricCategories(categories, name);
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.NOT_ENOUGH_CATEGORIES);
        }
    }

    @PutMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.OK)
    public void updateMetricsCategories (@RequestBody List<Map<String, String>> categories,@RequestParam(value = "name") String name) {
        try {
            metricsController.updateMetricCategory(categories, name);
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.NOT_ENOUGH_CATEGORIES);
        }
    }

    @DeleteMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMetricsCategories (@RequestParam(value = "name") String name) {
        try {
            metricsController.deleteMetricCategory(name);
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.NOT_ENOUGH_CATEGORIES);
        }
    }

    // PROJECT RELATED ENDPOINTS

    @GetMapping("/api/projects/{projectExternalId}/metrics")
    @ResponseStatus(HttpStatus.OK)
    public List<Metric> getMetrics(@PathVariable String projectExternalId) {
        try {
            return metricsController.getMetricsByProject(projectExternalId);
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CATEGORIES_DO_NOT_MATCH);
        }
    }
    @GetMapping("/api/projects/{projectExternalId}/metrics/students")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOStudentMetrics> getStudentsAndMetrics(@PathVariable String projectExternalId) throws IOException {

        return studentsController.getStudentMetricsFromProject(projectExternalId, null, null, null);
    }

    @GetMapping("/api/projects/{projectExternalId}/metrics/students/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOStudentMetrics> getStudentsAndMetricsHistorical(@PathVariable String projectExternalId,
                                                                             @RequestParam(value = "profile", required = false) String profileId,
                                                                             @RequestParam("from") String from,
                                                                             @RequestParam("to") String to) throws IOException {

        return studentsController.getStudentMetricsFromProject(projectExternalId, LocalDate.parse(from), LocalDate.parse(to), profileId);
    }

    @PutMapping("/api/projects/{projectExternalId}/metrics/students")
    @ResponseStatus(HttpStatus.OK)
    public Long updateMetricStudent(@PathVariable String projectExternalId, @RequestBody @Valid DTOCreateStudent body,
                                    Errors errors) {

        if(errors.hasErrors()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST + errors.getAllErrors().get(0).getDefaultMessage());
        }

        Map<DataSource, DTOStudentIdentity> parsedIdentities = new HashMap<>();
        body.getIdentities().forEach((dataSource, identity) -> {
            parsedIdentities.put(dataSource, new DTOStudentIdentity(dataSource, identity));
        });


        DTOStudent dtoStudent = new DTOStudent(body.getName(),  parsedIdentities);
        Long id = studentsController.updateStudentAndMetrics(body.getId(), dtoStudent, body.getMetrics(), projectExternalId);
        return id;
    }



    @RequestMapping("/api/projects/{projectExternalId}/metrics/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsEvaluations(@PathVariable String projectExternalId,
                                                           @RequestParam(value = "profile", required = false) String profile) {
        try {
            return metricsController.getAllMetricsCurrentEvaluation(projectExternalId, profile);
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/projects/{projectExternalId}/metrics/{metricId}/current")
    @ResponseStatus(HttpStatus.OK)
    public DTOMetricEvaluation getSingleMetricEvaluation(@PathVariable String projectExternalId,
                                                         @PathVariable String metricId) {
        try {
            return metricsController.getSingleMetricCurrentEvaluation(metricId, projectExternalId);
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/projects/{projectExternalId}/metrics/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsHistoricalData(@PathVariable String projectExternalId,
                                                              @RequestParam(value = "profile_id", required = false) String profileId,
                                                              @RequestParam("from") String from,
                                                              @RequestParam("to") String to) {
        try {
            return metricsController.getAllMetricsHistoricalEvaluation(projectExternalId, profileId, LocalDate.parse(from), LocalDate.parse(to));
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/projects/{projectExternalId}/metrics/{id}/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getHistoricalDataForMetric(@PathVariable String projectExternalId,
                                                                @RequestParam(value = "profile_id", required = false) String profileId,
                                                                @PathVariable String id, @RequestParam("from") String from,
                                                                @RequestParam("to") String to) {
        try {
            return metricsController.getSingleMetricHistoricalEvaluation(id, projectExternalId, profileId, LocalDate.parse(from), LocalDate.parse(to));
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/projects/{projectExternalId}/metrics/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsPredictionData(@PathVariable String projectExternalId,
                                                              @RequestParam(value = "profile_id", required = false) String profileId,
                                                              @RequestParam("technique") String techinique,
                                                              @RequestParam("horizon") String horizon) throws IOException {
        try {
            List<DTOMetricEvaluation> currentEvaluation = metricsController.getAllMetricsCurrentEvaluation(projectExternalId, profileId);
            return metricsController.getMetricsPrediction(currentEvaluation, projectExternalId, techinique, "7", horizon);
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/projects/{projectExternalId}/metrics/current-date")
    @ResponseStatus(HttpStatus.OK)
    public LocalDate getCurrentDate(@PathVariable String projectExternalId,
                                    @RequestParam(value = "profile_id", required = false) String profileId) {
        try {
            List<DTOMetricEvaluation> metrics = metricsController.getAllMetricsCurrentEvaluation(projectExternalId, profileId);
            return metrics.get(0).getDate();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        // if the response is null
        return null;
    }
}
