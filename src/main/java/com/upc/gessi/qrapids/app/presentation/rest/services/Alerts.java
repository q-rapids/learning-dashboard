package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.*;
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
                DTOAlert dtoAlert = new DTOAlert(a.getId(), a.getAffectedId(), a.getType(), a.getValue(), a.getThreshold(), new java.sql.Date(a.getDate().getTime()), a.getStatus(), a.getAffectedType());
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
            int newAlerts = alertsController.countNewAlerts(project.getId());
            return newAlerts;
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
        String affectedid = element.get("affected_id");
        String affectedType = element.get("affected_type");

        if (typeString != null && valueString != null && thresholdString != null && prj != null &&
                affectedid != null && affectedType != null) {
            try {
                AlertType type = AlertType.valueOf(typeString);
                float value = Float.parseFloat(valueString);
                float threshold = Float.parseFloat(thresholdString);
                Project project = projectsController.findProjectByExternalId(prj);

                alertsController.createAlert(value, threshold, type, project, affectedid, affectedType);

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
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.MISSING_ATTRIBUTES_IN_BODY);
        }
    }

}
