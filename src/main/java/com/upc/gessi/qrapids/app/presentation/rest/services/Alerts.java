package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.*;
import com.upc.gessi.qrapids.app.domain.exceptions.MetricNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.QualityFactorNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.StrategicIndicatorNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOAlert;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class Alerts {

    @Autowired
    private SimpMessagingTemplate smt;

    @Autowired
    private AlertsController alertsController;

    @Autowired
    private ProjectsController projectsController;

    @Autowired
    private MetricsController metricsController;

    @Autowired
    private FactorsController factorsController;

    @Autowired
    private StrategicIndicatorsController strategicIndicatorsController;

    private final Logger logger = LoggerFactory.getLogger(Alerts.class);

    @GetMapping("/api/alerts")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOAlert> getAlerts(@RequestParam(value = "prj") String prj) {
        try {
            Project project = projectsController.findProjectByExternalId(prj);
            List<Alert> alerts = alertsController.getAllProjectAlerts(project.getId());
            for (Alert alert:alerts) alertsController.changeAlertStatusToViewed(alert);

            List<DTOAlert> dtoAlerts = new ArrayList<>();
            for (Alert a : alerts) {
                DTOAlert dtoAlert = new DTOAlert(a.getId(), a.getAffectedId(),a.getAffectedType(), a.getType(), a.getValue(), a.getThreshold(), new java.sql.Date(a.getDate().getTime()), a.getStatus(), new java.sql.Date(a.getPredictionDate().getTime()), a.getPredictionTechnique());
                dtoAlerts.add(dtoAlert);
            }
            return dtoAlerts;
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }

    @GetMapping("/api/alerts/countNew")
    @ResponseStatus(HttpStatus.OK)
    public int countNewAlerts(@RequestParam(value = "prj") String prj) {
        try {
            Project project = projectsController.findProjectByExternalId(prj);
            return alertsController.countNewAlerts(project.getId());
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }

    @PostMapping("/api/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public void newAlert(@RequestBody Map<String, Map<String, String>> requestBody) {
        createAlert(requestBody);
    }

    private void createAlert (Map<String, Map<String, String>> requestBody) {
        Map<String, String> element = requestBody.get("element");

        String typeString = element.get("type");
        String valueString = element.get("value");
        String thresholdString = element.get("threshold");
        String prj = element.get("project_id");
        String affectedId = element.get("affectedId");
        String affectedType = element.get("affectedType");
        String predictionDate = element.get("predictionDate");
        String technique = element.get("technique");

        //check affected type is correct
        if (affectedType!=null && !affectedType.equals("metric") && !affectedType.equals("factor") && !affectedType.equals("indicator")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.AFFECTED_TYPE_NOT_VALID);
        }

        //check technique is correct
        List<String> forecastTechniques = strategicIndicatorsController.getForecastTechniques();
        if (!forecastTechniques.contains(technique)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.TECHNIQUE_NOT_VALID);

        //check predictionDate format is correct
        DateFormat formatter = new SimpleDateFormat("YYYY-MM-DD");
        Date prediction_date;
        try {
            prediction_date = (Date)formatter.parse(predictionDate);
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.BAD_FORMAT_DATE);
        }

        if (typeString != null && valueString != null && thresholdString != null && prj != null &&
                affectedId != null && affectedType != null && predictionDate!=null && technique !=null) {
            try {
                AlertType type = AlertType.valueOf(typeString);
                float value = Float.parseFloat(valueString);
                Float threshold = Float.parseFloat(thresholdString);
                Project project = projectsController.findProjectByExternalId(prj);

                alertsController.createAlert(value, threshold, type, project, affectedId, affectedType, prediction_date, technique);

                smt.convertAndSend(
                        "/queue/notify",
                        new Notification("New Alert")
                );
            } catch (ProjectNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more arguments have the wrong type");
            } catch (MetricNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.METRIC_NOT_FOUND);
            } catch (QualityFactorNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.FACTOR_NOT_FOUND);
            } catch (StrategicIndicatorNotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.STRATEGIC_INDICATOR_NOT_FOUND);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        }
    }

}
