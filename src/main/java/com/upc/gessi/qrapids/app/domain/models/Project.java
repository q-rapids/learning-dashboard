package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;

@Entity
@Table(name = "project")
public class Project {	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId", unique = true)
    private String externalId;
    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "logo")
    private byte[] logo;
    @Column(name = "active")
    private boolean active;
    @Column(name = "backlogId")
    private String backlogId;
    @Column(name = "isGlobal")
    private Boolean isGlobal;
    @Column(name = "anonymized")
    private boolean anonymized;

    public Project(){}
    
    public Project(String externalId, String name, String description, byte[] logo, boolean active, Boolean isGlobal) {
    	this.externalId = externalId;
    	this.name = name;
    	this.description = description;
    	this.logo = logo;
    	this.active = active;
        this.isGlobal=isGlobal;
        this.anonymized = false;
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

    public boolean isAnonymized() {
        return anonymized;
    }

    public void setAnonymized(boolean anonymized) {
        this.anonymized = anonymized;
    }
}
