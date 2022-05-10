package com.upc.gessi.qrapids.app.domain.repositories.Route;

import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.models.Route;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RouteRepository extends CrudRepository<Route, Long> {

    Route findByName(String name);

    List<Route> findAll();

}
