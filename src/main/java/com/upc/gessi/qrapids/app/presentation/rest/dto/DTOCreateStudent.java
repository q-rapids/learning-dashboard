package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DTOCreateStudent {

    List<Long> metrics = new ArrayList<>();
    Long id;

    String name;

    Map<DataSource, String> identities;

    public DTOCreateStudent(){}

    public List<Long> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Long> user_metrics) {
        this.metrics = user_metrics;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<DataSource, String> getIdentities() {
        return identities;
    }

    public void setIdentities(Map<DataSource, String> identities) {
        this.identities = identities;
    }
}
