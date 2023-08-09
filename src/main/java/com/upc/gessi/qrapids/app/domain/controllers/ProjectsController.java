package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.Backlog;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAProjects;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectAlreadyAnonymizedException;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.Profile.ProfileRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.ProjectIdentityRepository.ProjectIdentityRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOPhase;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectsController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectIdentityRepository projectIdentityRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private StudentsController studentsController;

    @Autowired
    private QMAProjects qmaProjects;

    @Autowired
    private Backlog backlog;

    public Project findProjectByExternalId (String externalId) throws ProjectNotFoundException {
        Project project = projectRepository.findByExternalId(externalId);
        if (project == null) {
            throw new ProjectNotFoundException();
        }
        return project;
    }

    public Map<DataSource,DTOProjectIdentity> getProjectIdentitiesByProject(Project project){
        List<ProjectIdentity> projectIdentities = projectIdentityRepository.findAllByProject(project);

        Map<DataSource,DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        projectIdentities.forEach(projectIdentity -> {
            dtoProjectIdentities.put(projectIdentity.getDataSource(),new DTOProjectIdentity(projectIdentity.getDataSource(), projectIdentity.getUrl()));
        });

        return dtoProjectIdentities;
    }

    public DTOProject getProjectByExternalId(String externalId) throws ProjectNotFoundException {
        Project project = projectRepository.findByExternalId(externalId);

        return getProjectDTO(project);
       }

    public List<DTOProject> getProjects(Long id) throws ProjectNotFoundException {
        List<DTOProject> projects = new ArrayList<>();
        Iterable<Project> projectIterable;
        if (id == null) { // Without Profile get all Projects
            projectIterable = projectRepository.findAll();
        } else { // With Profile get specific Projects
            Optional<Profile> profileOptional = profileRepository.findById(id);
            Profile profile = profileOptional.get();
            projectIterable = profile.getProjects();
        }
        List<Project> projectsBD = new ArrayList<>();
        projectIterable.forEach(projectsBD::add);
        for (Project p : projectsBD) {

            projects.add(getProjectDTO(p));
        }
        Collections.sort(projects, new Comparator<DTOProject>() {
            @Override
            public int compare(DTOProject o1, DTOProject o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return projects;
    }

    public Project getProjectById(Long projectId) throws ProjectNotFoundException {
        Optional<Project> projectOptional = projectRepository.findById(projectId);

        if (!projectOptional.isPresent())
            throw new ProjectNotFoundException();

        return projectOptional.get();
    }

    public DTOProject getProjectDTOById(Long projectId) throws ProjectNotFoundException {
        DTOProject dtoProject = null;

        Project project = getProjectById(projectId);
        List<DTOStudent> s = studentsController.getStudentsDTOFromProject(projectId);

        dtoProject = getProjectDTO(project);
        dtoProject.setStudents(s);

        return dtoProject;
    }

    //return true if a project with name not exists, if not also true in case that project contains given id
    public boolean checkProjectByName(Long id, String name) throws ProjectNotFoundException {
        Project p = projectRepository.findByName(name);
        return (p == null || p.getId() == id);
    }


    public void updateProjectIdentities(Collection<DTOProjectIdentity> dtoProjectIdentities, Project project){

        List<ProjectIdentity> identities = projectIdentityRepository.findAllByProject(project);

        Map<DataSource, ProjectIdentity> identityMap = new HashMap<>();

        identities.forEach(identity -> {
            identityMap.put(identity.getDataSource(), identity);
        });

        dtoProjectIdentities.forEach(identity -> {

            DataSource currentDataSource = identity.getDataSource();
            if(identityMap.containsKey(currentDataSource)){
                identityMap.get(currentDataSource).setUrl(identity.getUrl());
            }
            else {
                identityMap.put(currentDataSource,new ProjectIdentity(identity.getDataSource(), identity.getUrl(), project));
            }
        });

        projectIdentityRepository.saveAll(identityMap.values());
    }

    @Transactional
    public void updateProject(DTOProject dtoProject) {

        Project project = new Project(dtoProject.getExternalId(), dtoProject.getName(), dtoProject.getDescription(), dtoProject.getLogo(), dtoProject.getActive(), dtoProject.getIsGlobal());
        project.setId(dtoProject.getId());
        String backlogId = dtoProject.getBacklogId();

        if (backlogId == null || backlogId.equals("null")) backlogId = null;

        project.setBacklogId(backlogId);
        projectRepository.save(project);

        updateProjectIdentities(dtoProject.getIdentities().values(), project);
    }

    public List<String> getAllProjectsExternalID() throws IOException, CategoriesException {
        return qmaProjects.getAssessedProjects();
    }

    public List<String> importProjectsAndUpdateDatabase() throws IOException, CategoriesException {
        List<String> projects = getAllProjectsExternalID();
        updateDataBaseWithNewProjects(projects);
        return projects;
    }

    public void updateDataBaseWithNewProjects (List<String> projects) {
        for (String project : projects) {
            Project projectSaved = projectRepository.findByExternalId(project);
            if (projectSaved == null) {
                Project newProject = new Project(project, project, "No description specified", null, true, false);
                projectRepository.save(newProject);
            }
        }
    }

    public List<DTOMilestone> getMilestonesForProject (String projectExternalId, LocalDate date) throws ProjectNotFoundException {
        Project project = findProjectByExternalId(projectExternalId);
        return backlog.getMilestones(project.getBacklogId(), date);
    }

    public List<DTOPhase> getPhasesForProject (String projectExternalId, LocalDate date) throws ProjectNotFoundException {
        Project project = findProjectByExternalId(projectExternalId);
        return backlog.getPhases(project.getBacklogId(), date);
    }

    public DTOProject getProjectDTO(Project project){
        Map<DataSource,DTOProjectIdentity> dtoProjectIdentities = getProjectIdentitiesByProject(project);
        return new DTOProject(project.getId(), project.getExternalId(), project.getName(), project.getDescription(), project.getLogo(), project.getActive(), project.getBacklogId(), project.getIsGlobal(),dtoProjectIdentities, project.isAnonymized());
    }

    public DTOProject anonymizeProject(Long projectId) throws ProjectNotFoundException, ProjectAlreadyAnonymizedException {
        Project project = getProjectById(projectId);

        if(project.isAnonymized())
            throw new ProjectAlreadyAnonymizedException();

        studentsController.anonymizeStudentsFromProject(project);

        project.setAnonymized(true);
        projectRepository.save(project);

        return getProjectDTO(project);
    }

    @Transactional
    public List<DTOProject> anonymizeProjects(List<Long> projectIds) throws ProjectNotFoundException, ProjectAlreadyAnonymizedException {
        List<DTOProject> dtoProjects = new ArrayList<>();

        for(Long projectId : projectIds){
            dtoProjects.add(anonymizeProject(projectId));
        }

        return  dtoProjects;
    }
}
