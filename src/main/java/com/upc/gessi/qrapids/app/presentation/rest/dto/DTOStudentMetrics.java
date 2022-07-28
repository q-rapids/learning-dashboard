package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.util.List;

public class DTOStudentMetrics extends DTOStudent{

    private List<DTOMetricEvaluation> metrics;

    public DTOStudentMetrics(String student_name, String taiga_username, String github_username, List<DTOMetricEvaluation> metrics) {
        super(student_name,taiga_username,github_username);
        this.metrics=metrics;
    }

    public List<DTOMetricEvaluation> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DTOMetricEvaluation> metrics) {
        this.metrics = metrics;
    }
}
