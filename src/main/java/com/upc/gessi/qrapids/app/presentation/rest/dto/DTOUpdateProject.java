package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public class DTOUpdateProject {

    @JsonProperty("external_id")
    String externalId;

    String name;

    String description;

    @JsonProperty("backlog_id")
    String backlogId;

    MultipartFile logo;

    Map<DataSource, String> identities;

    Boolean global;

    public DTOUpdateProject(String externalId, String name, String description, String backlogId, MultipartFile logo, Map<DataSource, String> identities, Boolean global) {
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.backlogId = backlogId;
        this.logo = logo;
        this.identities = identities;
        this.global = global;
    }

    public DTOUpdateProject() {
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBacklogId() {
        return backlogId;
    }

    public void setBacklogId(String backlogId) {
        this.backlogId = backlogId;
    }

    public MultipartFile getLogo() {
        return logo;
    }

    public void setLogo(MultipartFile logo) {
        this.logo = logo;
    }

    public Map<DataSource, String> getIdentities() {
        return identities;
    }

    public void setIdentities(Map<DataSource, String> identities) {
        this.identities = identities;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }
}
