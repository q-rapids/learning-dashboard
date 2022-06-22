package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.exceptions.HistoricChartDatesNotFoundExeption;
import com.upc.gessi.qrapids.app.domain.models.HistoricDates;
import com.upc.gessi.qrapids.app.domain.models.ProjectHistoricDates;
import com.upc.gessi.qrapids.app.domain.repositories.Dates.HistoricDatesRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Dates.ProjectHistoricDatesRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOHistoricDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class IterationsController {

    @Autowired
    private ProjectHistoricDatesRepository projectHistoricDatesRepository;

    @Autowired
    private HistoricDatesRepository historicDatesRepository;

    public DTOHistoricDate getIterationsByIterationId(Long date_id) throws HistoricChartDatesNotFoundExeption {
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


    public List<DTOHistoricDate> getIterationsByProjectId(Long project_id) throws HistoricChartDatesNotFoundExeption {
        List<DTOHistoricDate> historicDatesDTO = new ArrayList<>();
        List<ProjectHistoricDates> projectHistoricDates = projectHistoricDatesRepository.findByProject_id(project_id);

        for(ProjectHistoricDates projectHistoricDate : projectHistoricDates) {
            historicDatesDTO.add(getIterationsByIterationId(projectHistoricDate.getDate_id()));
        }
        return historicDatesDTO;
    }

    public void createIteration(Map<String, String> dates, List<Long> project_ids) throws ParseException {
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

    public void updateIteration(Map<String, String> dates, List<Long> project_ids, Long dateId) throws ParseException {
        deleteIteration(dateId);
        createIteration(dates, project_ids);
    }

    public void deleteIteration(Long dateId) {
        historicDatesRepository.deleteById(dateId);
        projectHistoricDatesRepository.deleteByDate_id(dateId);
    }

    public List<DTOHistoricDate> getAllIterations() throws HistoricChartDatesNotFoundExeption {
        List<Long> ids = historicDatesRepository.getAllIds();
        List<DTOHistoricDate> dtoHistoricDates = new ArrayList<>();
        for(Long id : ids) {
            dtoHistoricDates.add(getIterationsByIterationId(id));
        }
        return dtoHistoricDates;
    }
}
