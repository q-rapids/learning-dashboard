package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.mongodb.MongoException;
import com.upc.gessi.qrapids.app.domain.controllers.MetricsController;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.FactorsController;
import com.upc.gessi.qrapids.app.domain.exceptions.*;
import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.QFCategory;
import com.upc.gessi.qrapids.app.domain.models.Factor;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.BadRequestException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
public class Factors {

    @Autowired
    private FactorsController factorsController;

    @Autowired
    private MetricsController metricsController;

    @Autowired
    private ProjectsController projectsController;

    private Logger logger = LoggerFactory.getLogger(Factors.class);

    @GetMapping("/api/qualityFactors/import")
    @ResponseStatus(HttpStatus.OK)
    public void importFactors() {
        try {
            factorsController.importFactorsAndUpdateDatabase();
        } catch (CategoriesException | ProjectNotFoundException | MetricNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CATEGORIES_DO_NOT_MATCH);
        } catch (IOException | MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on MongoDB connection");
        }
    }

    @GetMapping("api/factors/list")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getList() {

        return factorsController.getAllNames();

    }

    @GetMapping("/api/factors/categories")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactorCategory> getFactorCategories ( @RequestParam(value = "name", required = false) String name) {
        Iterable<QFCategory> factorCategoryList = factorsController.getFactorCategories(name);
        List<DTOFactorCategory> dtoFactorCategoryList = new ArrayList<>();
        for (QFCategory factorCategory : factorCategoryList) {
            dtoFactorCategoryList.add(new DTOFactorCategory(factorCategory.getId(), factorCategory.getName(), factorCategory.getColor(), factorCategory.getUpperThreshold(), factorCategory.getType()));
        }
        return dtoFactorCategoryList;
    }

    @PostMapping("/api/factors/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public void newFactorCategories (@RequestBody List<Map<String, String>> categories, @RequestParam(value = "name", required = false) String name) {
        if(categories.size()<1)
            throw new BadRequestException(Messages.NOT_ENOUGH_CATEGORIES);

        factorsController.newFactorCategories(categories, name);
    }

    @PutMapping("/api/factors/categories")
    @ResponseStatus(HttpStatus.OK)
    public void updateFactorsCategories (@RequestBody List<Map<String, String>> categories,@RequestParam(value = "name", required = true) String name) {
        factorsController.updateFactorCategory(categories, name);
    }

    @DeleteMapping("/api/factors/categories")
    @ResponseStatus(HttpStatus.OK)
    public void deleteFactorsCategories (@RequestParam(value = "name", required = true) String name) {
        factorsController.deleteFactorCategory(name);
    }

    @GetMapping("/api/qualityFactors")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactor> getAllQualityFactors (@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        List<Factor> factorsList = factorsController.getQualityFactorsByProjectAndProfile(prj, profile);
        List<DTOFactor> dtoFactorsList = new ArrayList<>();
        for (Factor factor : factorsList) {
            DTOFactor dtoFactor = new DTOFactor(factor.getId(),
                    factor.getExternalId(),
                    factor.getName(),
                    factor.getDescription(),
                    factor.getMetricsIds(),
                    factor.isWeighted(),
                    factor.getWeights(),
                    factor.getType(),
                    factor.getCategoryName());

            dtoFactor.setThreshold(factor.getThreshold());
            dtoFactorsList.add(dtoFactor);
        }
        return dtoFactorsList;
    }

    @GetMapping("/api/qualityFactors/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOFactor getQualityFactor(@PathVariable Long id) {
        try {
            Factor factor = factorsController.getQualityFactorById(id);
            DTOFactor dtoFactor = new DTOFactor(factor.getId(),
                        factor.getExternalId(),
                        factor.getName(),
                        factor.getDescription(),
                        factor.getMetricsIds(),
                        factor.isWeighted(),
                        factor.getWeights(),
                        factor.getType(),
                        factor.getCategoryName());

            dtoFactor.setThreshold(factor.getThreshold());
            return dtoFactor;
        } catch (QualityFactorNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(Messages.FACTOR_NOT_FOUND, id));
        }
    }

    @PostMapping("/api/qualityFactors")
    @ResponseStatus(HttpStatus.CREATED)
    public void newQualityFactor (HttpServletRequest request) {
        try {
            String prj = request.getParameter("prj");
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String threshold = request.getParameter("threshold");
            String type = request.getParameter("type");
            List<String> metrics = new ArrayList<>(Arrays.asList(request.getParameter("metrics").split(",")));
            String category = request.getParameter("category");
            if (!name.equals("") && !metrics.isEmpty()) {
                Project project = projectsController.findProjectByExternalId(prj);
                factorsController.saveQualityFactorWithCategory(name, description, threshold, metrics, type, category, project);

                if (!factorsController.assessQualityFactor(name, prj)) {
                    throw new AssessmentErrorException();
                }
            }
        }  catch (AssessmentErrorException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.ASSESSMENT_ERROR + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactorEvaluation> getQualityFactorsHistoricalData(@RequestParam(value = "prj", required=false) String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("from") String from, @RequestParam("to") String to) {
        try {
            return factorsController.getAllFactorsHistoricalEvaluation(prj, profile, LocalDate.parse(from), LocalDate.parse(to));
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PutMapping("/api/qualityFactors/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void editQualityFactor(@PathVariable Long id, HttpServletRequest request) {
        try {
            String name;
            String description;
            String threshold;
            String type;
            List<String> qualityMetrics;
            String category;
            try {
                name = request.getParameter("name");
                description = request.getParameter("description");
                threshold = request.getParameter("threshold");
                type=request.getParameter("type");
                qualityMetrics = new ArrayList<>(Arrays.asList(request.getParameter("metrics").split(",")));
                category = request.getParameter("category");
            } catch (Exception e) {
                throw new MissingParametersException();
            }
            if (!name.equals("") && !qualityMetrics.isEmpty()) {
                Factor oldFactor = factorsController.getQualityFactorById(id);
                factorsController.editQualityFactorWithCategory(oldFactor.getId(), name, description, threshold, qualityMetrics, type,category);
                if (!factorsController.assessQualityFactor(name, oldFactor.getProject().getExternalId())) {
                    throw new AssessmentErrorException();
                }
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new BadRequestException(Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (AssessmentErrorException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.ASSESSMENT_ERROR + e.getMessage());
        } catch (DataIntegrityViolationException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Integrity violation: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @DeleteMapping("/api/qualityFactors/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteQualityFactor (@PathVariable Long id) {
        try {
            factorsController.deleteFactor(id);
        } catch (QualityFactorNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DeleteFactorException e) {
            logger.error(e.getMessage(), e);
            // 403 - Forbidden
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, Messages.FACTOR_DELETE_FORBIDDEN);
        }
    }

    @GetMapping("/api/qualityFactors/metrics/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODetailedFactorEvaluation> getQualityFactorsEvaluations(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            return factorsController.getAllFactorsWithMetricsCurrentEvaluation(prj, profile, true);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException | ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/{id}/current")
    @ResponseStatus(HttpStatus.OK)
    public DTOFactorEvaluation getSingleFactorEvaluation (@RequestParam("prj") String prj, @PathVariable String id) {
        try {
            return factorsController.getSingleFactorEvaluation(id, prj);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/metrics/historical")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody
    List<DTODetailedFactorEvaluation> getDetailedQualityFactorsHistoricalData(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("from") String from, @RequestParam("to") String to) {
        try {
            return factorsController.getAllFactorsWithMetricsHistoricalEvaluation(prj,profile, LocalDate.parse(from), LocalDate.parse(to));
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException | ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactorEvaluation> getAllQualityFactorsEvaluation(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile) {
        try {
            return factorsController.getAllFactorsEvaluation(prj, profile, true);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/qualityFactors/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactorEvaluation> getQualityFactorsPredictionData(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("technique") String technique, @RequestParam("horizon") String horizon) throws IOException {
        try {
            List<DTOFactorEvaluation> currentEvaluation = factorsController.getAllFactorsEvaluation(prj, profile,true);
            return factorsController.getFactorsPrediction(currentEvaluation, prj, technique, "7", horizon);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/metrics/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODetailedFactorEvaluation> getQualityFactorsPrediction(@RequestParam(value = "prj") String prj, @RequestParam(value = "profile", required = false) String profile, @RequestParam("technique") String technique, @RequestParam("horizon") String horizon) {
        try {
            List<DTODetailedFactorEvaluation> currentEvaluation = factorsController.getAllFactorsWithMetricsCurrentEvaluation(prj, profile, true);
            return factorsController.getFactorsWithMetricsPrediction(currentEvaluation, technique, "7", horizon, prj);
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException | ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PostMapping("/api/qualityFactors/simulate")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOFactorEvaluation> simulate (@RequestParam("prj") String prj, @RequestParam("date") String date, @RequestBody List<DTOMetricEvaluation> metrics, @RequestParam(value = "profile", required=false) String profile) {
        try {
            Map<String, Float> metricsMap = new HashMap<>();
            for (DTOMetricEvaluation metric : metrics) {
                metricsMap.put(metric.getId(), metric.getValue());
            }
            return factorsController.simulate(metricsMap, prj, profile, LocalDate.parse(date));
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/qualityFactors/{id}/metrics/current")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODetailedFactorEvaluation> getMetricsCurrentEvaluationForQualityFactor(@RequestParam(value = "prj") String prj, @PathVariable String id) {
        try {
            List<DTOMetricEvaluation> metrics = metricsController.getMetricsForQualityFactorCurrentEvaluation(id, prj);
            DTOFactorEvaluation f = factorsController.getSingleFactorEvaluation(id,prj);
            List<DTODetailedFactorEvaluation> result = new ArrayList<>();
            DTODetailedFactorEvaluation df = new DTODetailedFactorEvaluation(id, f.getDescription(),f.getName(),metrics, f.getType());
            df.setValue(f.getValue());
            df.setDate(f.getDate());
            result.add(df);
            return result;
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/qualityFactors/{id}/metrics/historical")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODetailedFactorEvaluation> getMetricsHistoricalDataForQualityFactor(@RequestParam(value = "prj") String prj, @PathVariable String id, @RequestParam("from") String from, @RequestParam("to") String to) {
        try {
            List<DTOMetricEvaluation> metrics = metricsController.getMetricsForQualityFactorHistoricalEvaluation(id, prj, LocalDate.parse(from), LocalDate.parse(to));
            DTOFactorEvaluation f = factorsController.getSingleFactorEvaluation(id,prj);
            List<DTODetailedFactorEvaluation> result = new ArrayList<>();
            result.add(new DTODetailedFactorEvaluation(id,f.getDescription(),f.getName(),metrics, f.getType()));
            return result;
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @RequestMapping("/api/qualityFactors/{id}/metrics/prediction")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODetailedFactorEvaluation> getMetricsPredictionData(@RequestParam(value = "prj") String prj, @RequestParam("technique") String technique, @RequestParam("horizon") String horizon, @PathVariable String id) {
        try {
            List<DTOMetricEvaluation> currentEvaluation = metricsController.getMetricsForQualityFactorCurrentEvaluation(id, prj);
            List<DTOMetricEvaluation> metrics = metricsController.getMetricsPrediction(currentEvaluation, prj, technique, "7", horizon);
            DTOFactorEvaluation f = factorsController.getSingleFactorEvaluation(id,prj);
            List<DTODetailedFactorEvaluation> result = new ArrayList<>();
            result.add(new DTODetailedFactorEvaluation(id,f.getDescription(), f.getName(),metrics,f.getType()));
            return result;
        } catch (MongoException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Messages.PROJECT_NOT_FOUND, prj));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/qualityFactors/currentDate")
    @ResponseStatus(HttpStatus.OK)
    public LocalDate getcurrentDate(@RequestParam(value = "prj") String prj,@RequestParam(value = "profile", required = false) String profile) {
        try {
            List<DTOFactorEvaluation> qfs = factorsController.getAllFactorsEvaluation(prj, profile, true);
            return qfs.get(0).getDate();
        } catch (IOException | MongoException e) {
            logger.error(e.getMessage(), e);
        }
        // if the response is null
        return null;
    }
}
