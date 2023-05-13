package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.List;
import java.util.Map;

public class DTOStudentMetrics extends DTOStudent{

    private List<DTOMetricEvaluation> metrics;

    public DTOStudentMetrics(String student_name, Map<DataSource, DTOStudentIdentity> DTOStudentIdentities, List<DTOMetricEvaluation> metrics) {
        super(student_name,DTOStudentIdentities);
        this.metrics=metrics;
    }

    public List<DTOMetricEvaluation> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DTOMetricEvaluation> metrics) {
        this.metrics = metrics;
    }
}
