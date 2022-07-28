package com.upc.gessi.qrapids.app.domain.models;

import java.util.List;
import java.util.Map;

public class IterationAPIBody {

    List<Long> project_ids;
    Map<String, String> iteration;

    public IterationAPIBody() {
    }

    public IterationAPIBody(List<Long> project_ids, Map<String, String> iteration) {
        this.project_ids = project_ids;
        this.iteration = iteration;
    }

    public List<Long> getProject_ids() {
        return project_ids;
    }

    public void setProject_ids(List<Long> project_ids) {
        this.project_ids = project_ids;
    }

    public Map<String, String> getIteration() {
        return iteration;
    }

    public void setIteration(Map<String, String> iteration) {
        this.iteration = iteration;
    }
}
