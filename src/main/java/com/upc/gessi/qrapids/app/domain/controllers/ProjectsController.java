package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.adapters.Backlog;
import com.upc.gessi.qrapids.app.domain.adapters.QMA.QMAProjects;
import com.upc.gessi.qrapids.app.domain.exceptions.HistoricChartDatesNotFoundExeption;
import com.upc.gessi.qrapids.app.domain.models.HistoricDates;
import com.upc.gessi.qrapids.app.domain.models.Profile;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.domain.models.ProjectHistoricDates;
import com.upc.gessi.qrapids.app.domain.repositories.Dates.HistoricDatesRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Profile.ProfileRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Dates.ProjectHistoricDatesRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Student.StudentRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOPhase;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOHistoricDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectsController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private StudentsController studentsController;

    @Autowired
    private QMAProjects qmaProjects;

    @Autowired
    private ProjectHistoricDatesRepository projectHistoricDatesRepository;

    @Autowired
    private HistoricDatesRepository historicDatesRepository;

    @Autowired
    private Backlog backlog;

    public Project findProjectByExternalId (String externalId) throws ProjectNotFoundException {
        Project project = projectRepository.findByExternalId(externalId);
        if (project == null) {
            throw new ProjectNotFoundException();
        }
        return project;
    }

    public DTOProject getProjectByExternalId(String externalId) throws ProjectNotFoundException {
        Project p = projectRepository.findByExternalId(externalId);
        return new DTOProject(p.getId(), p.getExternalId(), p.getName(), p.getDescription(), p.getLogo(), p.getActive(), p.getBacklogId(), p.getTaigaURL(), p.getGithubURL(),p.getIsGlobal());
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
            DTOProject project = new DTOProject(p.getId(), p.getExternalId(), p.getName(), p.getDescription(), p.getLogo(), p.getActive(), p.getBacklogId(), p.getTaigaURL(), p.getGithubURL(),p.getIsGlobal());
            projects.add(project);
        }
        Collections.sort(projects, new Comparator<DTOProject>() {
            @Override
            public int compare(DTOProject o1, DTOProject o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return projects;
    }

    public DTOProject getProjectById(String id) throws ProjectNotFoundException {
        Optional<Project> projectOptional = projectRepository.findById(Long.parseLong(id));
        DTOProject dtoProject = null;
        if (projectOptional.isPresent()) {
            Project project = projectOptional.get();
            List<DTOStudent> s = studentsController.getStudentsFromProject(Long.parseLong(id));
            dtoProject = new DTOProject(project.getId(), project.getExternalId(), project.getName(), project.getDescription(), project.getLogo(), project.getActive(), project.getBacklogId(), project.getTaigaURL(), project.getGithubURL(), project.getIsGlobal());
            dtoProject.setStudents(s);
        }
        return dtoProject;
    }

    public boolean checkProjectByName(Long id, String name) throws ProjectNotFoundException {
        Project p = projectRepository.findByName(name);
        return (p == null || p.getId() == id);
    }

    public void updateProject(DTOProject p) {
        Project project = new Project(p.getExternalId(), p.getName(), p.getDescription(), p.getLogo(), p.getActive(), p.getTaigaURL(), p.getGithubURL(),p.getIsGlobal());
        project.setId(p.getId());
        project.setBacklogId(p.getBacklogId());
        projectRepository.save(project);
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
                Project newProject = new Project(project, project, "No description specified", null, true,null,null,false);
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

    public DTOHistoricDate getHistoricChartDatesByDateId(Long date_id) throws HistoricChartDatesNotFoundExeption {
        List<Long> project_ids = new ArrayList<>();
        Optional<HistoricDates> historicDate = historicDatesRepository.findById(date_id);
        if(!historicDate.isPresent()){
            throw new HistoricChartDatesNotFoundExeption();
        }
        List<ProjectHistoricDates> projectHistoricDates = projectHistoricDatesRepository.findByDate_id(date_id);
        for(ProjectHistoricDates projectHistoricDate : projectHistoricDates){
            project_ids.add(projectHistoricDate.getProject_id());
        }
        return new DTOHistoricDate(historicDate.get().getId(), historicDate.get().getName(),
                historicDate.get().getLabel(), historicDate.get().getFrom_date(), historicDate.get().getTo_date(), project_ids);
    }


    public List<DTOHistoricDate> getHistoricChartDatesByProjectId(Long project_id) throws HistoricChartDatesNotFoundExeption {
        List<DTOHistoricDate> historicDatesDTO = new ArrayList<>();
        List<ProjectHistoricDates> projectHistoricDates = projectHistoricDatesRepository.findByProject_id(project_id);

        for(ProjectHistoricDates projectHistoricDate : projectHistoricDates) {
            historicDatesDTO.add(getHistoricChartDatesByDateId(projectHistoricDate.getDate_id()));
        }
        return historicDatesDTO;
    }

    public void createHistoricDate(Map<String, String> dates, List<Long> project_ids) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date from_tmp = sdf.parse(dates.get("fromDate"));
        Date to_tmp = sdf.parse(dates.get("toDate"));

        HistoricDates newHistoricDate = new HistoricDates();
        newHistoricDate.setFrom_date(new java.sql.Date(from_tmp.getTime()));
        newHistoricDate.setTo_date(new java.sql.Date(to_tmp.getTime()));
        newHistoricDate.setName(dates.get("name"));
        newHistoricDate.setLabel(dates.get("label"));

        newHistoricDate = historicDatesRepository.save(newHistoricDate);
        historicDatesRepository.flush();

        for(Long project_id : project_ids) {
            ProjectHistoricDates newProjectHistoricDates = new ProjectHistoricDates();
            newProjectHistoricDates.setDate_id(newHistoricDate.getId());
            newProjectHistoricDates.setProject_id(project_id);
            projectHistoricDatesRepository.save(newProjectHistoricDates);
        }
    }

    public void updateHistoricDate(Map<String, String> dates, List<Long> project_ids, Long dateId) throws ParseException {
        deleteHistoricDate(dateId);
        createHistoricDate(dates, project_ids);
    }

    public void deleteHistoricDate(Long dateId) {
        historicDatesRepository.deleteById(dateId);
        projectHistoricDatesRepository.deleteByDate_id(dateId);
    }

    public List<DTOHistoricDate> getAllHistoricChartDates() throws HistoricChartDatesNotFoundExeption {
        List<Long> ids = historicDatesRepository.getAllIds();
        List<DTOHistoricDate> dtoHistoricDates = new ArrayList<>();
        for(Long id : ids) {
            dtoHistoricDates.add(getHistoricChartDatesByDateId(id));
        }
        return dtoHistoricDates;
    }
}
