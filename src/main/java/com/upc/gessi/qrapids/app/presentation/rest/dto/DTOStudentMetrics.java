package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.List;
import java.util.Map;

public class DTOStudentMetrics extends DTOStudent{

    private List<DTOMetricEvaluation> metrics;

    @JsonProperty("metrics_size")
    private Integer numberMetrics;
    public DTOStudentMetrics(String student_name, Map<DataSource, DTOStudentIdentity> identities, List<DTOMetricEvaluation> metrics) {
        super(student_name,identities);
        this.metrics=metrics;
        this.numberMetrics=null;
    }

    public DTOStudentMetrics(String student_name, Map<DataSource, DTOStudentIdentity> identities, List<DTOMetricEvaluation> metrics, Integer numberMetrics) {
        super(student_name,identities);
        this.metrics=metrics;
        this.numberMetrics=numberMetrics;
    }

    public List<DTOMetricEvaluation> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DTOMetricEvaluation> metrics) {
        this.metrics = metrics;
    }

    public Integer getNumberMetrics() {
        return numberMetrics;
    }

    public void setNumberMetrics(Integer numberMetrics) {
        this.numberMetrics = numberMetrics;
    }
}
