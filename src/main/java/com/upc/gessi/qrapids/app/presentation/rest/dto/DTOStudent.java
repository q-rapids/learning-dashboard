package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.Map;

public class DTOStudent {

    private Long id;

    private String name;

    private Map<DataSource, DTOStudentIdentity> identities;
    private DTOProject project;

    public DTOStudent() {

    }

    public DTOStudent( String student_name, Map<DataSource, DTOStudentIdentity> identities, DTOProject project) {
        this.id=null;
        this.name =student_name;
        this.identities=identities;
        this.project=project;
    }

    public DTOStudent(Long id, String student_name, Map<DataSource, DTOStudentIdentity> identities) {
        this.id=id;
        this.name =student_name;
        this.identities=identities;
        this.project=null;
    }

    public DTOStudent(String student_name, Map<DataSource, DTOStudentIdentity> identities) {
        this.id=null;
        this.name =student_name;
        this.identities=identities;
        this.project=null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {return this.name;}

    public void setName(String student_name) {this.name =student_name;}

    public Map<DataSource, DTOStudentIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(Map<DataSource, DTOStudentIdentity> identities) {
        this.identities = identities;
    }

    public DTOProject getProject() {
        return project;
    }

    public void setProject(DTOProject project) {
        this.project = project;
    }
}
