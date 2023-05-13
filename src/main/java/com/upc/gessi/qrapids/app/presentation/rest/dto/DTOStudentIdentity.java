package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.DataSource;

public class DTOStudentIdentity {

    private DataSource dataSource;
    private String username;
    private DTOStudent student;

    public DTOStudentIdentity() {
    }

    public DTOStudentIdentity(String username, DTOStudent student) {
        this.dataSource = null;
        this.username = username;
        this.student = student;
    }

    public DTOStudentIdentity(DataSource dataSource, String username) {
        this.dataSource = dataSource;
        this.username = username;
        this.student = null;
    }
    public DTOStudentIdentity(DataSource dataSource, String username, DTOStudent student) {
        this.dataSource = dataSource;
        this.username = username;
        this.student = student;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public DTOStudent getStudent() {
        return student;
    }

    public void setStudent(DTOStudent student) {
        this.student = student;
    }
}
