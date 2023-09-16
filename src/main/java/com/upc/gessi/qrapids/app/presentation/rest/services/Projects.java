package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.IterationsController;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.exceptions.ElementAlreadyPresentException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectAlreadyAnonymizedException;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.BadRequestException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


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
        Long id = null;
        if (profileId != null) id = Long.valueOf(profileId);
        return projectsController.getProjects(id);
    }

    @GetMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public DTOProject getProjectById(@PathVariable Long projectId) {
        return projectsController.getProjectDTOById(projectId);
    }

    @PutMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProject(@PathVariable Long projectId, @RequestPart("data") @Valid DTOUpdateProject body, @RequestPart(value = "file", required = false) MultipartFile multipartFile) throws IOException {

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

                DTOProject p = new DTOProject(projectId, body.getExternalId(), body.getName(), body.getDescription(), logoBytes, true, body.getBacklogId(), body.getGlobal(), parsedIdentities, project.isAnonymized());
                projectsController.updateProject(p);
            } else {
                throw new ElementAlreadyPresentException(String.format(Messages.PROJECT_NAME_ALREADY_EXISTS, body.getName()));
            }
    }

    @PostMapping("api/projects/{projectId}/anonymize")
    @ResponseStatus(HttpStatus.OK)
    public DTOProject anonymizeProject(@PathVariable Long projectId, @RequestBody(required = false) AnonymizationModes anonymizationMode) {

        AnonymizationModes mode = anonymizationMode;

        if (mode == null)
            mode = AnonymizationModes.COUNTRIES;

        Project project = projectsController.getProjectById(projectId);

        if(project.isAnonymized())
            throw new ProjectAlreadyAnonymizedException(projectId.toString());

        return projectsController.anonymizeProject(project, mode);

    }

    @PostMapping("api/projects/anonymize")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOProject> anonymizeProjects(@RequestBody @Valid DTOAnonymizeProjectsRequest body) {

        AnonymizationModes mode = body.getAnonymizationMode();

        if (mode == null)
            mode = AnonymizationModes.COUNTRIES;
        return projectsController.anonymizeProjects(body.getProjectIds(), mode);
    }

    @GetMapping("api/project/{projectId}/iterations") //!!!!!!!!!!!!!
    @ResponseStatus(HttpStatus.OK)
    public List<DTOIteration> getHistoricChartDates (@PathVariable Long projectId) {
            return iterationsController.getIterationsByProjectId(projectId);
    }

    @GetMapping("api/milestones")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOMilestone> getMilestones (@RequestParam("prj") String prj, @RequestParam(value = "date", required = false) String date) {

        LocalDate localDate = null;

        if (date != null) {
            localDate = LocalDate.parse(date);
        }

        return projectsController.getMilestonesForProject(prj, localDate);
    }

    @GetMapping("api/phases")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOPhase> getPhases (@RequestParam("prj") String prj, @RequestParam(value = "date", required = false) String date) {
        LocalDate localDate = null;
        if (date != null) {
            localDate = LocalDate.parse(date);
        }

        return projectsController.getPhasesForProject(prj, localDate);
    }

    @GetMapping("api/projects/identities")
    public List<DataSource> getIdentities(){
        return Arrays.asList(DataSource.values());
    }
}
