package com.upc.gessi.qrapids.app.domain.repositories.QFCategory;

import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.domain.models.QFCategory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface QFCategoryRepository extends CrudRepository<QFCategory, Long> {
    List<QFCategory> findAllByOrderByUpperThresholdAsc();

    Boolean existsByName(String name);

    List<QFCategory> findAllByName(String name);

    List<QFCategory> findAllByType(String type);

    QFCategory findByName(String name);

}
