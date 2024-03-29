package com.upc.gessi.qrapids.app.domain.repositories.QualityFactor;

import com.upc.gessi.qrapids.app.domain.models.Factor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface QualityFactorRepository extends CrudRepository<Factor, Long> {

    List<Factor> findByProject_IdOrderByName (Long projectId);

    List<Factor> findByProject_Id (Long projectId);

    //Factor findByExternalId(String externalId);

    Factor findByNameAndProject_Id (String name, Long projectId);

    Factor findByExternalIdAndProjectId(String factorExternalId,Long prjId);

    boolean existsByExternalIdAndProject_Id (String externalId, Long projectId);

    Optional<Factor> findByExternalId(String qualityFactorExternalId);

    Optional<Factor> findById(Long id);

    Optional<Factor> findByExternalIdAndProject_Id(String qualityFactorExternalId, Long projectId);
}
