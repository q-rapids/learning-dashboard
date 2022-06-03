package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.sql.Date;
import java.util.List;

public class DTOHistoricDate {
    private Long id;
    private String name;
    private String label;
    private Date from_date;
    private Date to_date;
    private List<Long> project_ids;

    public DTOHistoricDate(Long id, String name, String label, Date from_date, Date to_date, List<Long> project_ids) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.from_date = from_date;
        this.to_date = to_date;
        this.project_ids = project_ids;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Long> getProject_ids() {
        return project_ids;
    }

    public void setProject_ids(List<Long> project_ids) {
        this.project_ids = project_ids;
    }
}
