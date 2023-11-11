package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.mongodb.MongoException;
import com.upc.gessi.qrapids.app.domain.controllers.MetricsController;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.BadRequestException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        } catch (IOException | MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on MongoDB connection");
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
        String threshold = request.getParameter("threshold");
        String webUrl = request.getParameter("url");
        String categoryName = request.getParameter("categoryName");
        metricsController.editMetric(id,threshold,webUrl,categoryName); // at the moment is only possible change threshold}
    }


    @DeleteMapping("/api/metrics/students/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMetricStudent(@PathVariable Long id) {
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
        if(categories.size()<3) throw new BadRequestException(Messages.NOT_ENOUGH_CATEGORIES);
        else metricsController.newMetricCategories(categories, name);
    }

    @PutMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.OK)
    public void updateMetricsCategories (@RequestBody List<Map<String, String>> categories,@RequestParam(value = "name") String name) {
        metricsController.updateMetricCategory(categories, name);
    }

    @DeleteMapping("/api/metrics/categories")
    @ResponseStatus(HttpStatus.OK)
    public void deleteMetricsCategories (@RequestParam(value = "name") String name) {
        metricsController.deleteMetricCategory(name);
    }

    @GetMapping("/api/metrics")
    @ResponseStatus(HttpStatus.OK)
    public List<Metric> getMetrics(@RequestParam(value = "prj") String prj) {
        return metricsController.getMetricsByProject(prj);
    }

    @GetMapping("/api/metrics/students")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOStudentMetrics> getStudentsAndMetrics(@RequestParam(value = "prj") String prj) throws IOException {
        return studentsController.getStudentMetricsFromProject(prj, null, null, null);
    }

    @GetMapping("/api/metrics/students/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOStudentMetrics> getStudentsAndMetricsHistorical(@RequestParam(value = "prj") String prj,  @RequestParam(value = "profile", required = false) String profileId, @RequestParam("from") String from, @RequestParam("to") String to) throws IOException {
        return studentsController.getStudentMetricsFromProject(prj, LocalDate.parse(from), LocalDate.parse(to), profileId);
    }

    @PutMapping("/api/metrics/students")
    @ResponseStatus(HttpStatus.OK)
    public Long updateMetricStudent(@RequestParam(value="prj") String prj, @RequestBody @Valid DTOCreateStudent body) {

        Map<DataSource, DTOStudentIdentity> parsedIdentities = new HashMap<>();
        body.getIdentities().forEach((dataSource, identity) -> {
            parsedIdentities.put(dataSource, new DTOStudentIdentity(dataSource, identity));
        });


        DTOStudent dtoStudent = new DTOStudent(body.getName(),  parsedIdentities);
        Long id = studentsController.updateStudentAndMetrics(body.getId(), dtoStudent, body.getMetrics(), prj);
        return id;
    }



    @RequestMapping("/api/metrics/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsEvaluations(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            return metricsController.getAllMetricsCurrentEvaluation(prj, profile);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/metrics/{id}/current")
    @ResponseStatus(HttpStatus.OK)
    public DTOMetricEvaluation getSingleMetricEvaluation(@RequestParam("prj") String prj, @PathVariable String id) {
        try {
            return metricsController.getSingleMetricCurrentEvaluation(id, prj);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/metrics/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsHistoricalData(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("from") String from, @RequestParam("to") String to) {
        try {
            return metricsController.getAllMetricsHistoricalEvaluation(prj, profile, LocalDate.parse(from), LocalDate.parse(to));
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/metrics/{id}/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getHistoricalDataForMetric(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @PathVariable String id, @RequestParam("from") String from, @RequestParam("to") String to) {
        try {
            return metricsController.getSingleMetricHistoricalEvaluation(id, prj, profile, LocalDate.parse(from), LocalDate.parse(to));
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/metrics/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsPredictionData(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("technique") String technique, @RequestParam("horizon") String horizon) throws IOException {
        try {
            List<DTOMetricEvaluation> currentEvaluation = metricsController.getAllMetricsCurrentEvaluation(prj, profile);
            return metricsController.getMetricsPrediction(currentEvaluation, prj, technique, "7", horizon);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/metrics/currentDate")
    @ResponseStatus(HttpStatus.OK)
    public LocalDate getCurrentDate(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            List<DTOMetricEvaluation> metrics = metricsController.getAllMetricsCurrentEvaluation(prj, profile);
            return metrics.get(0).getDate();
        } catch (IOException | MongoException e) {
            logger.error(e.getMessage(), e);
        }
        // if the response is null
        return null;
    }
}
