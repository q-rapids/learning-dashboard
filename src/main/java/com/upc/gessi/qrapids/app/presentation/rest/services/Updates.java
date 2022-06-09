package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.config.libs.AuthTools;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.UpdatesController;
import com.upc.gessi.qrapids.app.domain.exceptions.UpdateNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.AppUser;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.COOKIE_STRING;
import static com.upc.gessi.qrapids.app.config.security.SecurityConstants.HEADER_STRING;

@RestController
public class Updates {

    @Autowired
    private UpdatesController updatesController;

    @GetMapping("/api/update")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOUpdate> getAllUpdate() {
        return updatesController.getUpdateList();
    }

    @GetMapping("/api/update/last")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOUpdate> getLastUpdate(@RequestParam(value = "username", required = false) String username) {
        //String cookie_token = this.authTools.getCookieToken( request, COOKIE_STRING );
        return updatesController.getLastUpdate(username);
    }

    @GetMapping("/api/update/year")
    @ResponseStatus(HttpStatus.OK)
    public List<DTOUpdate> getLastYearUpdates() {
        return updatesController.getUpdatesOfYear();
    }

    @GetMapping("/api/update/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DTOUpdate getUpdate(@PathVariable Long id) throws UpdateNotFoundException {
        return updatesController.getUpdateById(id);
    }


    @PostMapping("/api/update")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUpdate(HttpServletRequest request) {

        String name = request.getParameter("name");
        String date = request.getParameter("date");
        String update = request.getParameter("update");
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        LocalDate localdate = LocalDate.parse(date);
        DTOUpdate dto = new DTOUpdate(name, localdate, update);
        updatesController.newUpdate(dto);

    }

    @PutMapping("/api/update/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateUpdate(HttpServletRequest request, @PathVariable Long id ) throws UpdateNotFoundException {

        String name = request.getParameter("name");
        String date = request.getParameter("date");
        String update = request.getParameter("update");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localdate= LocalDate.parse(date, formatter);
        DTOUpdate dto = new DTOUpdate(id,name, localdate, update);
        updatesController.updateUpdateById(dto);

    }

    @DeleteMapping("/api/update/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUpdate(@PathVariable Long id) throws UpdateNotFoundException {

        updatesController.deleteUpdateById(id);

    }
}
