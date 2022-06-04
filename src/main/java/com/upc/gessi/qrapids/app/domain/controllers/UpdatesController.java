package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.exceptions.MetricNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.UpdateNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.Update;
import com.upc.gessi.qrapids.app.domain.repositories.Update.UpdateRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UpdatesController {

    @Autowired
    private UpdateRepository updateRepository;

    //Get update name and id
    public List<DTOUpdate> getUpdateList() {
        List<Update> updateList = updateRepository.findAll();
        List<DTOUpdate> dtoUpdateList = new ArrayList<>();
        for(Update u : updateList) {
            DTOUpdate temp = new DTOUpdate(u.getId(), u.getName());
            dtoUpdateList.add(temp);
        }
        return dtoUpdateList;
    }

    //Get full update with certain id
    public DTOUpdate getUpdateById(Long id) throws UpdateNotFoundException {
        Optional<Update> u = updateRepository.findById(id);
        if(u.isPresent()) {
            Update update = u.get();
            DTOUpdate dtoUpdate= new DTOUpdate(update.getId(), update.getName(), update.getDate(), update.getUpdate());
            return dtoUpdate;
        }else {
            throw new UpdateNotFoundException();
        }
    }

    //Get updates of this year? aqui o a js
    public List<DTOUpdate> getUpdatesOfYear() {
        LocalDate now = LocalDate.now();
        LocalDate firstDay = now.with(firstDayOfYear());
        List<Update> updateList = updateRepository.findAllByDateAfterOrderByDateAsc(firstDay);
        List<DTOUpdate> dtoUpdateList = new ArrayList<>();
        for(Update u : updateList) {
            DTOUpdate dtoUpdate= new DTOUpdate(u.getId(), u.getName(), u.getDate(), u.getUpdate());
            dtoUpdateList.add(dtoUpdate);
        }
        return dtoUpdateList;
    }

    //Get last update?? aqui o a js
    public DTOUpdate getLastUpdate() {
        List<Update> update = updateRepository.findAll();
        DTOUpdate dtoUpdate= new DTOUpdate(update.get(0).getId(), update.get(0).getName(), update.get(0).getDate(), update.get(0).getUpdate());
        return dtoUpdate;

    }

    //New update
    public void newUpdate(DTOUpdate dtoUpdate) {
        Update update = new Update();
        update.setName(dtoUpdate.getName());
        update.setUpdate(dtoUpdate.getUpdate());
        update.setDate(dtoUpdate.getDate());
        updateRepository.save(update);
    }

    //Update update
    public void updateUpdateById(DTOUpdate dtoUpdate) throws UpdateNotFoundException {
        Optional<Update> u = updateRepository.findById(dtoUpdate.getId());
        if(u.isPresent()) {
            Update update = u.get();
            update.setName(dtoUpdate.getName());
            update.setDate(dtoUpdate.getDate());
            update.setUpdate(dtoUpdate.getUpdate());
            updateRepository.save(update);
        }else {
            throw new UpdateNotFoundException();
        }
    }

    //delete update
    public void deleteUpdateById(Long id) throws UpdateNotFoundException {
        Optional<Update> u = updateRepository.findById(id);
        if(u.isPresent()) {
            Update update = u.get();
            updateRepository.delete(update);
        }else {
            throw new UpdateNotFoundException();
        }
    }

}
