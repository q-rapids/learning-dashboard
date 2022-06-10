package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.util.List;

public class DTOStudentMetricsHistorical extends DTOStudent{

    private List<List<DTOMetricEvaluation>> metricsHistorical;

    public DTOStudentMetricsHistorical(String student_name, String taiga_username, String github_username, List<List<DTOMetricEvaluation>> metricsHistorical) {
        super(student_name,taiga_username,github_username);
        this.metricsHistorical=metricsHistorical;
    }

    public List<List<DTOMetricEvaluation>> getMetrics() {
        return metricsHistorical;
    }

    public void setMetrics(List<List<DTOMetricEvaluation>> metricsHistorical) {
        this.metricsHistorical = metricsHistorical;
    }

}
