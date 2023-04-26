package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.util.List;

public class DTOStudentMetricsHistorical extends DTOStudent{

    private List<DTOMetricEvaluation> metrics;
    private Integer numberMetrics;

    public DTOStudentMetricsHistorical(String student_name, String taiga_username, String github_username, String prt_username, List<DTOMetricEvaluation> metrics, Integer numberMetrics) {
        super(student_name,taiga_username,github_username, prt_username);
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
