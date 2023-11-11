package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;

import java.util.List;

public class DTOAnonymizeProjectRequest {
    @JsonProperty("anonymization_mode")
    private AnonymizationModes anonymizationMode;

    public DTOAnonymizeProjectRequest() {
    }

    public DTOAnonymizeProjectRequest(AnonymizationModes anonymizationMode) {
        this.anonymizationMode = anonymizationMode;
    }

    public AnonymizationModes getAnonymizationMode() {
        return anonymizationMode;
    }

    public void setAnonymizationMode(AnonymizationModes anonymizationMode) {
        this.anonymizationMode = anonymizationMode;
    }
}
