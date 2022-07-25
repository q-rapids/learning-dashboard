package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.IterationsController;
import com.upc.gessi.qrapids.app.domain.models.HistoricDateAPIBody;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOHistoricDate;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class Iterations {

    @Autowired
    IterationsController iterationsController;

    private Logger logger = LoggerFactory.getLogger(Iterations.class);

    @GetMapping("api/iterations")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOHistoricDate> getIterations () {
        try {
            return iterationsController.getAllIterations();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PostMapping("api/iterations")
    @ResponseStatus(HttpStatus.CREATED)
    public void newIterations(@RequestBody HistoricDateAPIBody body) {
        try {
            if(body.getIteration() == null || body.getProject_ids() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            iterationsController.createIteration(body.getIteration(), body.getProject_ids());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @PutMapping("api/iterations/{iteration_id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateIterations(@RequestBody HistoricDateAPIBody body, @PathVariable Long iteration_id) {
        try {
            if(body.getIteration() == null || body.getProject_ids() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            iterationsController.updateIteration(body.getIteration(), body.getProject_ids(), iteration_id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }

    @DeleteMapping("api/iterations/{iteration_id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteIterations(@PathVariable Long iteration_id) {
        try {
            iterationsController.deleteIteration(iteration_id);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, Messages.INTERNAL_SERVER_ERROR + e.getMessage());
        }
    }
}
