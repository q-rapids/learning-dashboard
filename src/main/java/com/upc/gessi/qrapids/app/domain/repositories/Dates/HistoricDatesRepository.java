package com.upc.gessi.qrapids.app.domain.repositories.Dates;

import com.upc.gessi.qrapids.app.domain.models.HistoricDates;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface HistoricDatesRepository extends CrudRepository<HistoricDates, Long> {
    @Query("select h.id from HistoricDates h")
    List<Long> getAllIds();
     void flush();
}
