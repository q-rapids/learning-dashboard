package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;

import java.util.List;

public class DTOAnonymizeProjectsRequest {

    @JsonProperty("project_ids")
    private List<Long> projectIds;

    @JsonProperty("anonymization_mode")
    private AnonymizationModes anonymizationMode;

    public DTOAnonymizeProjectsRequest() {
    }

    public DTOAnonymizeProjectsRequest(List<Long> projectIds, AnonymizationModes anonymizationMode) {
        this.projectIds = projectIds;
        this.anonymizationMode = anonymizationMode;
    }

    public List<Long> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<Long> projectIds) {
        this.projectIds = projectIds;
    }

    public AnonymizationModes getAnonymizationMode() {
        return anonymizationMode;
    }

    public void setAnonymizationMode(AnonymizationModes anonymizationMode) {
        this.anonymizationMode = anonymizationMode;
    }
}
