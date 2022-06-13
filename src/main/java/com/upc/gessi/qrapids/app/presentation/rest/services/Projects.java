package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.google.gson.JsonObject;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.exceptions.ElementAlreadyPresentException;
import com.upc.gessi.qrapids.app.domain.models.HistoricDateAPIBody;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOMilestone;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOPhase;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudent;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOHistoricDate;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class Projects {

    @Autowired
    private ProjectsController projectsController;

    @Autowired
    private StudentsController studentsController;

    private Logger logger = LoggerFactory.getLogger(Projects.class);

    @GetMapping("/api/projects/import")
    @ResponseStatus(HttpStatus.OK)
    public List<String> importProjects() {
    	try {
            return projectsController.importProjectsAndUpdateDatabase();
        } catch (CategoriesException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.CATEGORIES_DO_NOT_MATCH);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on ElasticSearch connection");
        }
    }

    @GetMapping("/api/projects")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOProject> getProjects(@RequestParam(value = "profile_id", required = false) String profileId) {
        try {
            Long id = null;
            if (profileId != null) id = Long.valueOf(profileId);
            return projectsController.getProjects(id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("/api/projects/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOProject getProjectById(@PathVariable String id) {
        try {
            DTOProject p = projectsController.getProjectById(id);
            return p;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PutMapping("/api/projects/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProject(@PathVariable Long id, HttpServletRequest request, @RequestParam(value = "logo", required = false) MultipartFile logo) {
        try {
            String externalId = request.getParameter("externalId");
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String backlogId = request.getParameter("backlogId");
            String taigaURL= request.getParameter("taigaURL");
            if(taigaURL.equals("null")) taigaURL=null;
            String githubURL= request.getParameter("githubURL");
            if(githubURL.equals("null")) githubURL=null;
            Boolean isGlobal = Boolean.parseBoolean(request.getParameter("isGlobal"));
            byte[] logoBytes = null;
            if (logo != null) {
                logoBytes = IOUtils.toByteArray(logo.getInputStream());
            }
            if (logoBytes != null && logoBytes.length < 10) {
                DTOProject p = projectsController.getProjectById(Long.toString(id));
                logoBytes = p.getLogo();
            }
            if (projectsController.checkProjectByName(id, name)) {
                DTOProject p = new DTOProject(id, externalId, name, description, logoBytes, true, backlogId, taigaURL, githubURL, isGlobal);
                projectsController.updateProject(p);
            } else {
                throw new ElementAlreadyPresentException();
            }
        } catch (ElementAlreadyPresentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("api/project/{project_id}/historicdates")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOHistoricDate> getHistoricChartDates (@PathVariable Long project_id) {
        try {
            return projectsController.getHistoricChartDatesByProjectId(project_id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("api/project/historicdates")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOHistoricDate> getHistoricChartDates () {
        try {
            return projectsController.getAllHistoricChartDates();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PostMapping("api/project/historicdates")
    @ResponseStatus(HttpStatus.CREATED)
    public void newHistoricChartDates (@RequestBody HistoricDateAPIBody body) {
        try {
            if(body.getIteration() == null || body.getProject_ids() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            projectsController.createHistoricDate(body.getIteration(), body.getProject_ids());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PutMapping("api/project/historicdates/{historic_date_id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateHistoricChartDates (@RequestBody HistoricDateAPIBody body, @PathVariable Long historic_date_id) {
        try {
            if(body.getIteration() == null || body.getProject_ids() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            projectsController.updateHistoricDate(body.getIteration(), body.getProject_ids(), historic_date_id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @DeleteMapping("api/project/historicdates/{historic_date_id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteHistoricChartDates (@PathVariable Long historic_date_id) {
        try {
            projectsController.deleteHistoricDate(historic_date_id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @GetMapping("api/milestones")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMilestone> getMilestones (@RequestParam("prj") String prj, @RequestParam(value = "date", required = false) String date) {
        LocalDate localDate = null;
        if (date != null) {
            localDate = LocalDate.parse(date);
        }
        try {
            return projectsController.getMilestonesForProject(prj, localDate);
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }

    @GetMapping("api/phases")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOPhase> getPhases (@RequestParam("prj") String prj, @RequestParam(value = "date", required = false) String date) {
        LocalDate localDate = null;
        if (date != null) {
            localDate = LocalDate.parse(date);
        }
        try {
            return projectsController.getPhasesForProject(prj, localDate);
        } catch (ProjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.PROJECT_NOT_FOUND);
        }
    }
}
