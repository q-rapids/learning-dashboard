package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.IterationsController;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.exceptions.ElementAlreadyPresentException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectAlreadyAnonymizedException;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;


@RestController
public class Projects {

    @Autowired
    private ProjectsController projectsController;

    @Autowired
    private StudentsController studentsController;

    @Autowired
    private IterationsController iterationsController;

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

    @GetMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public DTOProject getProjectById(@PathVariable Long projectId) {
        try {
            DTOProject p = projectsController.getProjectDTOById(projectId);
            return p;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PutMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProject(@PathVariable Long projectId, @RequestPart("data") @Valid DTOUpdateProject body, Errors errors, @RequestPart(value = "file", required = false) MultipartFile multipartFile) {
        try {

            if(errors.hasErrors()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST + errors.getAllErrors().get(0).getDefaultMessage());
            }

            DTOProject project = projectsController.getProjectDTOById(projectId);

            byte[] logoBytes = null;
            if (multipartFile != null) {
                logoBytes = IOUtils.toByteArray(multipartFile.getInputStream());
            }

            if (logoBytes != null && logoBytes.length < 10) {
                logoBytes = project.getLogo();
            }

            if (projectsController.checkProjectByName(projectId, body.getName())) {

                Map<DataSource, DTOProjectIdentity> parsedIdentities = new HashMap<>();
                body.getIdentities().forEach((dataSource, identity) -> {
                    parsedIdentities.put(dataSource, new DTOProjectIdentity(dataSource, identity));
                });

                System.out.println(body.getDescription() + Arrays.toString(logoBytes) + body.getBacklogId());
                DTOProject p = new DTOProject(projectId, body.getExternalId(), body.getName(), body.getDescription(), logoBytes, true, body.getBacklogId(), body.getGlobal(), parsedIdentities, project.isAnonymized());
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

    @PostMapping("api/projects/{projectId}/anonymize")
    @ResponseStatus(HttpStatus.OK)
    public DTOProject anonymizeProject(@PathVariable Long projectId) {
        try {
            Project project = projectsController.getProjectById(projectId);
            if(project.isAnonymized())
                throw new ProjectAlreadyAnonymizedException();
            return projectsController.anonymizeProject(project);
        } catch (ProjectNotFoundException e){
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.PROJECT_NOT_FOUND);
        } catch (ProjectAlreadyAnonymizedException e){
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.PROJECT_ALREADY_ANONYMIZED);
        }
    }

    @PostMapping("api/projects/anonymize")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOProject> anonymizeProjects(@RequestBody @Valid List<Long> projectIds, Errors errors) {
        try {
            if(errors.hasErrors()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST + errors.getAllErrors().get(0).getDefaultMessage());
            }
            return projectsController.anonymizeProjects(projectIds);
        } catch (ProjectNotFoundException e){
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.PROJECT_NOT_FOUND);
        } catch (ProjectAlreadyAnonymizedException e){
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.PROJECT_ALREADY_ANONYMIZED);
        }
    }

    @GetMapping("api/project/{projectId}/iterations")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOIteration> getHistoricChartDates (@PathVariable Long projectId) {
        try {
            return iterationsController.getIterationsByProjectId(projectId);
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.PROJECT_NOT_FOUND);
        }
    }

    @GetMapping("api/projects/identities")
    public List<DataSource> getIdentities(){
        return Arrays.asList(DataSource.values());
    }
}
