package com.upc.gessi.qrapids.app.presentation.rest.dto;

import org.apache.tomcat.jni.Local;

import java.time.LocalDate;

public class DTOUpdate {

    private Long id;
    private String name;
    private LocalDate date;
    private String update;

    public DTOUpdate(Long id, String name) {
        this.id=id;
        this.name=name;
    }

    public DTOUpdate(Long id, String name, LocalDate date, String update) {
        this.id=id;
        this.name=name;
        this.date=date;
        this.update=update;
    }

    public DTOUpdate(String name, LocalDate date, String update) {
        this.name=name;
        this.date=date;
        this.update=update;
    }


    public Long getId() {return id;}

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {return name;}

    public void setName(String name) {  this.name = name;}

    public LocalDate getDate() {return date;}

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getUpdate() {return update;}

    public void setUpdate(String update) { this.update = update; }


}
