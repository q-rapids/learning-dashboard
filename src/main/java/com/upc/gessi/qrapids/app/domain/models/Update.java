package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "update")
public class Update {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name")
    private String name;

    @Column(name="date")
    private LocalDate date;

    @Column(name="update", columnDefinition = "TEXT")
    private String update;

    public Update(String name, LocalDate date, String update) {
        this.name = name;
        this.date=date;
        this.update=update;
    }

    public Update() {

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
