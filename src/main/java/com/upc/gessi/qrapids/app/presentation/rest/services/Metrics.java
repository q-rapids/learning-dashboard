package com.upc.gessi.qrapids.app.presentation.rest.services;


import com.upc.gessi.qrapids.app.domain.controllers.MetricsController;
import com.upc.gessi.qrapids.app.domain.exceptions.MetricNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.Metric;
import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOCategoryThreshold;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMetricCategory;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMetricEvaluation;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.elasticsearch.ElasticsearchStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class Metrics {

    @Autowired
    private MetricsController metricsController;

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

    @GetMapping("/api/metrics")
    @ResponseStatus(HttpStatus.OK)
    public List<Metric> getMetrics(@RequestParam(value = "prj") String prj) {
        try {
            return metricsController.getMetricsByProject(prj);
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CATEGORIES_DO_NOT_MATCH);
        }
    }

    @GetMapping("api/metrics/list")
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
            boolean exists=metricsController.CheckIfNameExists(name);
            if(exists) throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.NOT_ENOUGH_CATEGORIES);
            else metricsController.newMetricCategories(categories, name);
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.NOT_ENOUGH_CATEGORIES);
        }
    }

    @RequestMapping("/api/metrics/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsEvaluations(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            return metricsController.getAllMetricsCurrentEvaluation(prj, profile);
        } catch (ElasticsearchStatusException e) {
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
        } catch (ElasticsearchStatusException e) {
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
        } catch (ElasticsearchStatusException e) {
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
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/metrics/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMetricEvaluation> getMetricsPredictionData(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("technique") String techinique, @RequestParam("horizon") String horizon) throws IOException {
        try {
            List<DTOMetricEvaluation> currentEvaluation = metricsController.getAllMetricsCurrentEvaluation(prj, profile);
            return metricsController.getMetricsPrediction(currentEvaluation, prj, techinique, "7", horizon);
        } catch (ElasticsearchStatusException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/metrics/currentDate")
    @ResponseStatus(HttpStatus.OK)
    public LocalDate getcurrentDate(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            List<DTOMetricEvaluation> metrics = metricsController.getAllMetricsCurrentEvaluation(prj, profile);
            return metrics.get(0).getDate();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        // if the response is null
        return null;
    }
}
