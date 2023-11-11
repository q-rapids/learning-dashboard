package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.DataSource;

public class DTOProjectIdentity {

    private DataSource dataSource;
    private String url;
    private DTOProject project;

    public DTOProjectIdentity() {
    }

    public DTOProjectIdentity(String url, DTOProject project) {
        this.dataSource = null;
        this.url = url;
        this.project = project;
    }

    public DTOProjectIdentity(DataSource dataSource, String url) {
        this.dataSource = dataSource;
        this.url = url;
        this.project = null;
    }
    public DTOProjectIdentity(DataSource dataSource, String url, DTOProject project) {
        this.dataSource = dataSource;
        this.url = url;
        this.project = project;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public DTOProject getProject() {
        return project;
    }

    public void setProject(DTOProject project) {
        this.project = project;
    }
}
