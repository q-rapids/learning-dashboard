package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.QRPatternsController;
import com.upc.gessi.qrapids.app.domain.controllers.QualityRequirementController;
import com.upc.gessi.qrapids.app.domain.controllers.UsersController;
import com.upc.gessi.qrapids.app.domain.exceptions.ElementAlreadyPresentException;
import com.upc.gessi.qrapids.app.domain.exceptions.MissingParametersException;
import com.upc.gessi.qrapids.app.domain.exceptions.QRPatternNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.QRPatternsMetricInUseException;
import com.upc.gessi.qrapids.app.domain.models.Alert;
import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.QualityRequirement;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOQRPatternsClassifier;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOQRPatternsMetric;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Mappers;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOAlert;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOQualityRequirement;
import com.upc.gessi.qrapids.app.presentation.rest.dto.qrPattern.DTOQRPattern;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import qr.models.Classifier;
import qr.models.Metric;
import qr.models.QualityRequirementPattern;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class QualityRequirements {

    @Autowired
    private ProjectsController projectsController;

    @Autowired
    private QualityRequirementController qualityRequirementController;

    @Autowired
    private QRPatternsController qrPatternsController;

    @Autowired
    private UsersController usersController;

    private Logger logger = LoggerFactory.getLogger(QualityRequirements.class);

    @PostMapping("/api/qr/ignore")
    @ResponseStatus(HttpStatus.CREATED)
    public void ignoreQR (@RequestParam(value = "prj") String prj, HttpServletRequest request) {
        String rationale = request.getParameter("rationale");
        String patternId = request.getParameter("patternId");
        try {
            Project project = projectsController.findProjectByExternalId(prj);
            qualityRequirementController.ignoreQualityRequirement(project, rationale, Integer.parseInt(patternId));
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }

    @PostMapping("/api/qr")
    @ResponseStatus(HttpStatus.CREATED)
    public DTOQualityRequirement newQR (@RequestParam(value = "prj") String prj, HttpServletRequest request, Authentication authentication) {
        try {
            String rationale = request.getParameter("rationale");
            String patternId = request.getParameter("patternId");
            AppUser user = null;
            if (authentication != null) {
                String author = authentication.getName();
                user = usersController.findUserByName(author);
            }

            String requirement = request.getParameter("requirement");
            String description = request.getParameter("description");
            String goal = request.getParameter("goal");

            Project project = projectsController.findProjectByExternalId(prj);
            QualityRequirement qualityRequirement = qualityRequirementController.addQualityRequirement(requirement, description, goal, rationale, Integer.parseInt(patternId), user, project);

            return new DTOQualityRequirement(
                    qualityRequirement.getId(),
                    new java.sql.Date(qualityRequirement.getDecision().getDate().getTime()),
                    qualityRequirement.getRequirement(),
                    qualityRequirement.getDescription(),
                    qualityRequirement.getGoal(),
                    qualityRequirement.getBacklogId(),
                    qualityRequirement.getBacklogUrl());
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }catch (HttpClientErrorException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error when saving the quality requirement in the backlog");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/api/qr")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOQualityRequirement> getQRs(@RequestParam(value = "prj") String prj) {
        try {
            Project project = projectsController.findProjectByExternalId(prj);
            List<QualityRequirement> qualityRequirements = qualityRequirementController.getAllQualityRequirementsForProject(project);
            List<DTOQualityRequirement> dtoQualityRequirements = new ArrayList<>();
            for (QualityRequirement qualityRequirement : qualityRequirements) {
                DTOQualityRequirement dtoQualityRequirement = new DTOQualityRequirement(
                        qualityRequirement.getId(),
                        new java.sql.Date(qualityRequirement.getDecision().getDate().getTime()),
                        qualityRequirement.getRequirement(),
                        qualityRequirement.getDescription(),
                        qualityRequirement.getGoal(),
                        qualityRequirement.getBacklogId(),
                        qualityRequirement.getBacklogUrl());

                Alert alert = qualityRequirement.getAlert();
                if (alert != null) {
                    DTOAlert dtoAlert = new DTOAlert(
                            alert.getId(),
                            alert.getId_element(),
                            alert.getName(),
                            alert.getType(),
                            alert.getValue(),
                            alert.getThreshold(),
                            alert.getCategory(),
                            new java.sql.Date(alert.getDate().getTime()),
                            alert.getStatus(),
                            alert.isReqAssociat(),
                            null);
                    dtoQualityRequirement.setAlert(dtoAlert);
                }

                dtoQualityRequirement.setBacklogProjectId(project.getBacklogId());

                dtoQualityRequirements.add(dtoQualityRequirement);
            }
            return dtoQualityRequirements;
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }

    @GetMapping("/api/qrPatterns")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOQRPattern> getAllQRPatterns () {
        List<QualityRequirementPattern> qualityRequirementPatternList = qrPatternsController.getAllPatterns();
        List<DTOQRPattern> dtoQRPatternList = new ArrayList<>();
        for (QualityRequirementPattern qrPattern : qualityRequirementPatternList) {
            dtoQRPatternList.add(Mappers.mapQualityRequirementPatternToDTOQRPattern(qrPattern));
        }
        return dtoQRPatternList;
    }

    @GetMapping("/api/qrPatterns/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOQRPattern getQRPattern (@PathVariable String id) {
        QualityRequirementPattern qualityRequirementPattern = qrPatternsController.getOnePattern(Integer.parseInt(id));
        return Mappers.mapQualityRequirementPatternToDTOQRPattern(qualityRequirementPattern);
    }

    @GetMapping("/api/qrPatterns/{id}/metric")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getMetricsForQRPattern (@PathVariable String id) {
        String metric = qrPatternsController.getMetricForPattern(Integer.parseInt(id));
        Map<String, String> object = new HashMap<>();
        object.put("metric", metric);
        return object;
    }

    /**
     * Create a new quality requirement pattern.
     * @param request HTTP request containing the new pattern attributes
     */
    @PostMapping("/api/qrPatterns")
    @ResponseStatus(HttpStatus.CREATED)
    public void createQRPattern(HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String goal = request.getParameter("goal");
            String description = request.getParameter("description");
            String requirement = request.getParameter("requirement");
            String classifierId = request.getParameter("classifierId");
            String classifierName = request.getParameter("classifierName");
            String classifierPos = request.getParameter("classifierPos");
            String classifierPatterns = request.getParameter("classifierPatterns");
            String parameterName = request.getParameter("parameterName");
            String parameterDescription = request.getParameter("parameterDescription");
            String parameterCorrectnessCondition = request.getParameter("parameterCorrectnessCondition");
            String metricId = request.getParameter("metricId");
            if (name == null || classifierId == null || classifierName == null || classifierPos == null || classifierPatterns == null || parameterName == null || metricId == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                QualityRequirementPattern newPattern = Mappers.mapToQualityRequirementPattern(name, goal, description, requirement, parameterName, parameterDescription, parameterCorrectnessCondition, Integer.parseInt(metricId));
                List<Integer> listClassifierPatterns = new ArrayList<>();
                if (!classifierPatterns.equals("")) {
                    for (String pat : classifierPatterns.split(",")) {
                        listClassifierPatterns.add(Integer.parseInt(pat));
                    }
                }
                if (!qrPatternsController.createPattern(newPattern, Integer.parseInt(classifierId), classifierName, Integer.parseInt(classifierPos), listClassifierPatterns)) {
                    throw new ElementAlreadyPresentException();
                }
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (ElementAlreadyPresentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pattern name already exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Edit a quality requirement pattern.
     * @param id Pattern identifier
     * @param request HTTP request containing the edited pattern attributes
     */
    @PutMapping("/api/qrPatterns/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateQRPattern(@PathVariable String id, HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String goal = request.getParameter("goal");
            String description = request.getParameter("description");
            String requirement = request.getParameter("requirement");
            String classifierId = request.getParameter("classifierId");
            String classifierName = request.getParameter("classifierName");
            String classifierPos = request.getParameter("classifierPos");
            String classifierPatterns = request.getParameter("classifierPatterns");
            String parameterName = request.getParameter("parameterName");
            String parameterDescription = request.getParameter("parameterDescription");
            String parameterCorrectnessCondition = request.getParameter("parameterCorrectnessCondition");
            String metricId = request.getParameter("metricId");
            if (name == null || classifierId == null || classifierName == null || classifierPos == null || classifierPatterns == null || parameterName == null || metricId == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                QualityRequirementPattern pattern = Mappers.mapToQualityRequirementPattern(name, goal, description, requirement, parameterName, parameterDescription, parameterCorrectnessCondition, Integer.parseInt(metricId));
                List<Integer> listClassifierPatterns = new ArrayList<>();
                for (String pat : classifierPatterns.split(",")) {
                    listClassifierPatterns.add(Integer.parseInt(pat));
                }
                if (!qrPatternsController.editPattern(Integer.parseInt(id), pattern, Integer.parseInt(classifierId), classifierName, Integer.parseInt(classifierPos), listClassifierPatterns)) {
                    throw new ElementAlreadyPresentException();
                }
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (ElementAlreadyPresentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pattern name already exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Delete a quality requirement pattern.
     * @param id Pattern identifier
     */
    @DeleteMapping("/api/qrPatterns/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteQRPattern(@PathVariable String id) {
        try {
            qrPatternsController.deletePattern(Integer.parseInt(id));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Get all the classifiers.
     * @return List containing all the classifiers
     */
    @GetMapping("/api/qrPatternsClassifiers")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOQRPatternsClassifier> getAllQRPatternsClassifiers() {
        List<Classifier> classifierList = qrPatternsController.getAllClassifiers();
        List<DTOQRPatternsClassifier> dtoClassifierList = new ArrayList<>();
        for (Classifier classifier : classifierList) {
            dtoClassifierList.add(Mappers.mapClassifierToDTOQRPatternsClassifier(classifier));
        }
        return dtoClassifierList;
    }

    /**
     * Get one classifier.
     * @param id Classifier identifier
     * @return Classifier with the given identifier
     */
    @GetMapping("/api/qrPatternsClassifiers/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOQRPatternsClassifier getQRPatternsClassifier(@PathVariable String id) {
        Classifier classifier = qrPatternsController.getOneClassifier(Integer.parseInt(id));
        return Mappers.mapClassifierToDTOQRPatternsClassifier(classifier);
    }

    /**
     * Create a new classifier.
     * @param request HTTP request containing the new classifier attributes
     */
    @PostMapping("/api/qrPatternsClassifiers")
    @ResponseStatus(HttpStatus.CREATED)
    public void createQRPatternsClassifier(HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String parentClassifierId = request.getParameter("parentClassifier");
            if (name == null || parentClassifierId == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                qrPatternsController.createClassifier(name, Integer.parseInt(parentClassifierId));
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Edit a classifier
     * @param id Classifier identifier
     * @param request HTTP request containing the edited classifier attributes
     */
    @PutMapping("/api/qrPatternsClassifiers/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateQRPatternsClassifier(@PathVariable String id, HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String oldParentClassifierId = request.getParameter("oldParentClassifier");
            String parentClassifierId = request.getParameter("parentClassifier");
            if (name == null || oldParentClassifierId == null || parentClassifierId == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                qrPatternsController.updateClassifier(Integer.parseInt(id), name, Integer.parseInt(oldParentClassifierId), Integer.parseInt(parentClassifierId));
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Delete a classifier.
     * @param id Classifier identifier
     */
    @DeleteMapping("/api/qrPatternsClassifiers/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteQRPatternsClassifier(@PathVariable String id) {
        try {
            qrPatternsController.deleteClassifier(Integer.parseInt(id));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Get all the metrics.
     * @return List containing all the metrics
     */
    @GetMapping("/api/qrPatternsMetrics")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOQRPatternsMetric> getAllQRPatternsMetrics() {
        List<Metric> metricList = qrPatternsController.getAllMetrics();
        List<DTOQRPatternsMetric> dtoMetricList = new ArrayList<>();
        for (Metric metric : metricList) {
            dtoMetricList.add(Mappers.mapMetricToDTOQRPatternsMetric(metric));
        }
        return dtoMetricList;
    }

    /**
     * Get one metric.
     * @param id Metric identifier
     * @return Metric with the given identifier
     */
    @GetMapping("/api/qrPatternsMetrics/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOQRPatternsMetric getQRPatternsMetric(@PathVariable String id) {
        Metric metric = qrPatternsController.getOneMetric(Integer.parseInt(id));
        return Mappers.mapMetricToDTOQRPatternsMetric(metric);
    }

    /**
     * Create a new metric.
     * @param request HTTP request containing the new metric attributes
     */
    @PostMapping("/api/qrPatternsMetrics")
    @ResponseStatus(HttpStatus.CREATED)
    public void createQRPatternsMetric(HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String type = request.getParameter("type");
            if (name == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                Metric newMetric = new Metric();
                newMetric.setName(name);
                newMetric.setDescription(description);
                newMetric.setType(type);
                if (type.equals("integer") || type.equals("float")) {
                    String minValue = request.getParameter("minValue");
                    String maxValue = request.getParameter("maxValue");
                    if (minValue != null && !minValue.equals("")) {
                        newMetric.setMinValue(Float.valueOf(minValue));
                    }
                    if (maxValue != null && !maxValue.equals("")) {
                        newMetric.setMaxValue(Float.valueOf(maxValue));
                    }
                } else if (type.equals("domain")) {
                    String possibleValues = request.getParameter("possibleValues");
                    possibleValues = possibleValues.replace("\r", "");
                    List<String> listPossibleValues = new ArrayList<>();
                    for (String val : possibleValues.split("\n")) {
                        listPossibleValues.add(val);
                    }
                    newMetric.setPossibleValues(listPossibleValues);
                }
                if (!qrPatternsController.createMetric(newMetric)) {
                    throw new ElementAlreadyPresentException();
                }
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (ElementAlreadyPresentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Metric name already exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Edit a metric.
     * @param id Metric identifier
     * @param request HTTP request containing the edited metric attributes
     */
    @PutMapping("/api/qrPatternsMetrics/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateQRPatternsMetric(@PathVariable String id, HttpServletRequest request) {
        try {
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String type = request.getParameter("type");
            if (name == null) {
                throw new MissingParametersException();
            }
            if (!name.equals("")) {
                Metric editedMetric = new Metric();
                editedMetric.setName(name);
                editedMetric.setDescription(description);
                editedMetric.setType(type);
                if (type.equals("integer") || type.equals("float")) {
                    String minValue = request.getParameter("minValue");
                    String maxValue = request.getParameter("maxValue");
                    if (minValue != null && !minValue.equals("")) {
                        editedMetric.setMinValue(Float.valueOf(minValue));
                    }
                    if (maxValue != null && !maxValue.equals("")) {
                        editedMetric.setMaxValue(Float.valueOf(maxValue));
                    }
                } else if (type.equals("domain")) {
                    String possibleValues = request.getParameter("possibleValues");
                    possibleValues = possibleValues.replace("\r", "");
                    List<String> listPossibleValues = new ArrayList<>();
                    for (String val : possibleValues.split("\n")) {
                        listPossibleValues.add(val);
                    }
                    editedMetric.setPossibleValues(listPossibleValues);
                }
                if (!qrPatternsController.updateMetric(Integer.parseInt(id), editedMetric)) {
                    throw new ElementAlreadyPresentException();
                }
            }
        } catch (MissingParametersException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        } catch (ElementAlreadyPresentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Metric name already exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    /**
     * Delete a metric.
     * @param id Metric identifier
     */
    @DeleteMapping("/api/qrPatternsMetrics/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteQRPatternsMetric(@PathVariable String id) {
        try {
            if (!qrPatternsController.deleteMetric(Integer.parseInt(id))) {
                throw new QRPatternsMetricInUseException();
            }
        } catch (QRPatternsMetricInUseException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This metric is used in a pattern");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

}
