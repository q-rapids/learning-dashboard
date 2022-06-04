package com.upc.gessi.qrapids.app.domain.repositories.Update;

import com.upc.gessi.qrapids.app.domain.models.Update;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UpdateRepository  extends CrudRepository<Update, Long> {

    List<Update> findAll();

    Optional<Update> findById(Long id);

    //List<Update> findAllOrderByDateDesc();

    List<Update> findAllByDateAfterOrderByDateAsc(LocalDate date);
}
