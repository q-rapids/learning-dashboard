package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.DecisionsController;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.models.Decision;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTODecision;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class Decisions {

    @Autowired
    private DecisionsController decisionsController;

    @Autowired
    private ProjectsController projectsController;

    private Logger logger = LoggerFactory.getLogger(Decisions.class);

    @GetMapping("/api/decisions")
    @ResponseStatus(HttpStatus.OK)
    public List<DTODecision> getDecisions (@RequestParam(value = "prj") String prj, @RequestParam(required = false, defaultValue = "false") boolean qrs, @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        try {
            LocalDate fromDate = LocalDate.ofEpochDay(0);
            if (from != null) {
                fromDate = LocalDate.parse(from);
            }
            LocalDate toDate = LocalDate.now();
            if (to != null) {
                toDate = LocalDate.parse(to);
            }
            toDate = toDate.plusDays(1);

            Project project = projectsController.findProjectByExternalId(prj);
            List<DTODecision> dtoDecisions = new ArrayList<>();
            if (qrs) {
                dtoDecisions.addAll(decisionsController.getAllDecisionsWithQRByProjectAndDates(project, Date.valueOf(fromDate), Date.valueOf(toDate)));
            } else {
                List<Decision> decisions = decisionsController.getAllDecisionsByProject(project);
                for (Decision decision : decisions) {
                    String username = "";
                    if (decision.getAuthor() != null)
                        username = decision.getAuthor().getUsername();
                    DTODecision dtoDecision = new DTODecision(decision.getId(), decision.getType(), new Date(decision.getDate().getTime()), username, decision.getRationale(), decision.getPatternId());
                    dtoDecisions.add(dtoDecision);
                }
            }
            return dtoDecisions;
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }


}
