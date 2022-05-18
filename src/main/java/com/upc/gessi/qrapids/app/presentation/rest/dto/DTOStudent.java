package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.Project;

public class DTOStudent {
    private Long id;
    private String name;
    private String taiga_username;
    private String github_username;
    private DTOProject project;

    public DTOStudent(String name, String taiga_username, String github_username, DTOProject project) {
        this.name=name;
        this.taiga_username=taiga_username;
        this.github_username=github_username;
        this.project=project;
    }
    public String getName() {return this.name;}

    public void setName(String name) {this.name=name;}

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
