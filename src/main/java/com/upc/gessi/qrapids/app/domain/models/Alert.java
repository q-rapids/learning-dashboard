package com.upc.gessi.qrapids.app.domain.models;

import org.hibernate.annotations.CollectionId;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "Alert")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    @Column(name = "status")
    private AlertStatus status;
    @Column(name = "value")
    private float value;
    @Column(name = "threshold")
    private float threshold;
    @Column(name = "type")
    private AlertType type;
    @ManyToOne
    @JoinColumn(name="projectid", referencedColumnName = "id")
    private Project project;
    @Column(name = "affectedid")
    private String affectedId;
    @Column(name = "affectedtype")
    private String affectedType;

    public Alert (float value, float threshold, AlertType type, Project project, String affectedId, String affectedType) {
        this.value = value;
        this.threshold = threshold;
        this.type = type;
        this.project = project;
        this.affectedId = affectedId;
        this.affectedType = affectedType;
        date =  new Date();
        status = AlertStatus.NEW;

    };

    public Alert() {
    }

    public Long getId(){
        return id;
    }
    public void setId (Long alertId) {
        id = alertId;
    }

    public Date getDate(){
        return date;
    }
    public void setDate( Date alertDate){
        date = alertDate;
    }

    public AlertStatus getStatus() {return status;}
    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public float getValue() {
        return value;
    }
    public void setValue(float alertValue) {
        value = alertValue;
    }

    public float getThreshold() {
        return threshold;
    }
    public void setThreshold(float alertThreshold) {
        threshold = alertThreshold;
    }

    public AlertType getType() {
        return type;
    }
    public void setType(AlertType alertType) {
        type = alertType;
    }

    public Project getProject() {return project;}
    public void setProject(Project project) {
        this.project = project;
    }

    public String getAffectedId() {
        return affectedId;
    }
    public void setAffectedId(String affectedId) {
        this.affectedId = affectedId;
    }

    public String getAffectedType() {return affectedType;}
    public void setAffectedType(String affectedType){this.affectedType=affectedType;}
}
