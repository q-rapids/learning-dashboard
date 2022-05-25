package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.sql.Date;

public class DTOProjectHistoricDate {
    private Long id;
    private String name;
    private Long project_id;
    private Date from_date;
    private Date to_date;

    public DTOProjectHistoricDate(Long id, String name, Long project_id, Date from_date, Date to_date) {
        this.id = id;
        this.name = name;
        this.project_id = project_id;
        this.from_date = from_date;
        this.to_date = to_date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getProject_id() {
        return project_id;
    }

    public void setProject_id(Long project_id) {
        this.project_id = project_id;
    }

    public Date getFrom_date() {
        return from_date;
    }

    public void setFrom_date(Date from_date) {
        this.from_date = from_date;
    }

    public Date getTo_date() {
        return to_date;
    }

    public void setTo_date(Date to_date) {
        this.to_date = to_date;
    }
}
