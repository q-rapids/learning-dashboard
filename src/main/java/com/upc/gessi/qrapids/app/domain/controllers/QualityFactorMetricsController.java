package com.upc.gessi.qrapids.app.domain.controllers;

import com.upc.gessi.qrapids.app.domain.exceptions.QualityFactorMetricsNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.*;
import com.upc.gessi.qrapids.app.domain.repositories.QualityFactor.QualityFactorMetricsRepository;
import com.upc.gessi.qrapids.app.domain.repositories.QualityFactor.QualityFactorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QualityFactorMetricsController {

    @Autowired
    private QualityFactorMetricsRepository qualityFactorMetricsRepository;

    @Autowired
    private QualityFactorRepository qualityFactorRepository;

    public QualityFactorMetrics saveQualityFactorMetric(Float weight, Metric metric, Factor qf) {
        QualityFactorMetrics qualityFactorMetric;
        qualityFactorMetric = new QualityFactorMetrics(weight, metric, qf);
        qualityFactorMetricsRepository.save(qualityFactorMetric);
        return qualityFactorMetric;
    }

    public void deleteQualityFactorMetric(Long id) throws QualityFactorMetricsNotFoundException {
        if (qualityFactorMetricsRepository.existsById(id)) {
            qualityFactorMetricsRepository.deleteById(id);
        } else {
            throw new QualityFactorMetricsNotFoundException();
        }
    }

    public String getTypeFromFactorOfMetric(Metric metric) {
        List<QualityFactorMetrics> qfm = qualityFactorMetricsRepository.findByMetric(metric);
        Optional<Factor> f = qualityFactorRepository.findById(qfm.get(0).getQuality_factor().getId());
        if(f.isPresent()) {
            Factor ftemp = f.get();
            return ftemp.getType();
        }
        return null;
    }


}
