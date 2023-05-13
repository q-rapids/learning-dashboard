package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.DataSource;

import java.util.Map;

public class DTOStudent {
    private Long student_id;
    private String student_name;

    private Map<DataSource, DTOStudentIdentity> identities;
    private DTOProject project;

    public DTOStudent() {

    }

    public DTOStudent( String student_name, Map<DataSource, DTOStudentIdentity> identities, DTOProject project) {
        this.student_id=null;
        this.student_name=student_name;
        this.identities=identities;
        this.project=project;
    }

    public DTOStudent(Long id, String student_name, Map<DataSource, DTOStudentIdentity> identities) {
        this.student_id=id;
        this.student_name=student_name;
        this.identities=identities;
        this.project=null;
    }

    public DTOStudent(String student_name, Map<DataSource, DTOStudentIdentity> identities) {
        this.student_id=null;
        this.student_name=student_name;
        this.identities=identities;
        this.project=null;
    }

    public Long getStudent_id() {return this.student_id;}

    public void setStudent_id(Long id) {this.student_id=id;}

    public String getStudentName() {return this.student_name;}

    public void setStudentName(String student_name) {this.student_name=student_name;}

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
