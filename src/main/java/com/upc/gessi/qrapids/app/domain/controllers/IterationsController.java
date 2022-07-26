package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.exceptions.HistoricChartDatesNotFoundExeption;
import com.upc.gessi.qrapids.app.domain.models.Iteration;
import com.upc.gessi.qrapids.app.domain.models.ProjectIterations;
import com.upc.gessi.qrapids.app.domain.repositories.Iteration.IterationRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Iteration.ProjectIterationsRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOIteration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class IterationsController {

    @Autowired
    private ProjectIterationsRepository projectIterationsRepository;

    @Autowired
    private IterationRepository iterationRepository;

    public DTOIteration getIterationsByIterationId(Long date_id) throws HistoricChartDatesNotFoundExeption {
        List<Long> project_ids = new ArrayList<>();
        Optional<Iteration> historicDate = iterationRepository.findById(date_id);
        if(!historicDate.isPresent()){
            throw new HistoricChartDatesNotFoundExeption();
        }
        List<ProjectIterations> projectHistoricDates = projectIterationsRepository.findByDate_id(date_id);
        for(ProjectIterations projectHistoricDate : projectHistoricDates){
            project_ids.add(projectHistoricDate.getProject_id());
        }
        return new DTOIteration(historicDate.get().getId(), historicDate.get().getName(),
                historicDate.get().getLabel(), historicDate.get().getFrom_date(), historicDate.get().getTo_date(), project_ids);
    }


    public List<DTOIteration> getIterationsByProjectId(Long project_id) throws HistoricChartDatesNotFoundExeption {
        List<DTOIteration> historicDatesDTO = new ArrayList<>();
        List<ProjectIterations> projectHistoricDates = projectIterationsRepository.findByProject_id(project_id);

        for(ProjectIterations projectHistoricDate : projectHistoricDates) {
            historicDatesDTO.add(getIterationsByIterationId(projectHistoricDate.getDate_id()));
        }
        return historicDatesDTO;
    }

    public void createIteration(Map<String, String> dates, List<Long> project_ids) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date from_tmp = sdf.parse(dates.get("fromDate"));
        Date to_tmp = sdf.parse(dates.get("toDate"));

        Iteration newHistoricDate = new Iteration();
        newHistoricDate.setFrom_date(new java.sql.Date(from_tmp.getTime()));
        newHistoricDate.setTo_date(new java.sql.Date(to_tmp.getTime()));
        newHistoricDate.setName(dates.get("name"));
        newHistoricDate.setLabel(dates.get("label"));

        newHistoricDate = iterationRepository.save(newHistoricDate);
        iterationRepository.flush();

        for(Long project_id : project_ids) {
            ProjectIterations newProjectIterations = new ProjectIterations();
            newProjectIterations.setDate_id(newHistoricDate.getId());
            newProjectIterations.setProject_id(project_id);
            projectIterationsRepository.save(newProjectIterations);
        }
    }

    public void updateIteration(Map<String, String> iteration, List<Long> project_ids, Long dateId) throws ParseException {
        deleteIteration(dateId);
        createIteration(iteration, project_ids);
    }

    public void deleteIteration(Long dateId) {
        iterationRepository.deleteById(dateId);
        projectIterationsRepository.deleteByDate_id(dateId);
    }

    public List<DTOIteration> getAllIterations() throws HistoricChartDatesNotFoundExeption {
        List<Long> ids = iterationRepository.getAllIds();
        List<DTOIteration> dtoIterations = new ArrayList<>();
        for(Long id : ids) {
            dtoIterations.add(getIterationsByIterationId(id));
        }
        return dtoIterations;
    }
}
