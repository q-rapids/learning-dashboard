package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.Project;

public class DTOStudent {
    private Long student_id;
    private String student_name;
    private String taiga_username;
    private String github_username;
    private DTOProject project;

    public DTOStudent() {

    }

    public DTOStudent( String student_name, String taiga_username, String github_username, DTOProject project) {
        this.student_id=null;
        this.student_name=student_name;
        this.taiga_username=taiga_username;
        this.github_username=github_username;
        this.project=project;
    }

    public DTOStudent(Long id, String student_name, String taiga_username, String github_username) {
        this.student_id=id;
        this.student_name=student_name;
        this.taiga_username=taiga_username;
        this.github_username=github_username;
        this.project=null;
    }

    public DTOStudent(String student_name, String taiga_username, String github_username) {
        this.student_id=null;
        this.student_name=student_name;
        this.taiga_username=taiga_username;
        this.github_username=github_username;
        this.project=null;
    }

    public Long getStudent_id() {return this.student_id;}

    public void setStudent_id(Long id) {this.student_id=id;}

    public String getStudentName() {return this.student_name;}

    public void setStudentName(String student_name) {this.student_name=student_name;}

    public String getTaigaUsername() {return this.taiga_username;}

    public void setTaigaUsername(String taiga_username) {this.taiga_username=taiga_username;}

    public String getGithubUsername() {return this.github_username;}

    public void setGithubUsername(String github_username) {this.github_username=github_username;}

    public DTOProject getProject() {
        return project;
    }

    public void setProject(DTOProject project) {
        this.project = project;
    }
}
