package com.upc.gessi.qrapids.app.domain.repositories.Dates;

import com.upc.gessi.qrapids.app.domain.models.Iteration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HistoricDatesRepository extends CrudRepository<Iteration, Long> {
    @Query("select i.id from Iteration i")
    List<Long> getAllIds();
     void flush();
}
