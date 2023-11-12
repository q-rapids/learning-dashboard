package com.upc.gessi.qrapids.app.presentation.rest.dto;


import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.List;
import java.util.Map;

public class DTOProject {
    private Long id;
    private String externalId;
    private String name;
    private String description;
    private byte[] logo;
    private boolean active;
    private String backlogId;
    private Boolean isGlobal;
    private boolean anonymized;

    private Map<DataSource, DTOProjectIdentity> identities;
    private List<DTOStudent> students;
    
    public DTOProject(){}
    
    public DTOProject(Long id, String externalId, String name, String description, byte[] logo, boolean active, String backlogId, Boolean isGlobal, Map<DataSource, DTOProjectIdentity> identities, boolean anonymized) {
    	this.id = id;
    	this.externalId = externalId;
    	this.name = name;
    	this.description = description;
    	this.logo = logo;
    	this.active = active;
    	this.backlogId = backlogId;
    	this.isGlobal=isGlobal;
        this.identities = identities;
        this.anonymized = anonymized;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setexternalId(String externalId) {
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

    public byte[] getLogo() {
        return logo;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }
    
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBacklogId() {
        return backlogId;
    }

    public void setBacklogId(String backlogId) {
        this.backlogId = backlogId;
    }

    public Boolean getIsGlobal() { return isGlobal;}

    public void setIsGlobal(Boolean global) { isGlobal = global;}

    public List<DTOStudent> getStudents() { return students; }

    public void setStudents(List<DTOStudent> students) {this.students=students;}

    public Map<DataSource, DTOProjectIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(Map<DataSource, DTOProjectIdentity> identities) {
        this.identities = identities;
    }

    public boolean isAnonymized() {
        return anonymized;
    }

    public void setAnonymized(boolean anonymized) {
        this.anonymized = anonymized;
    }
}
