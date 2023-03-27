package com.upc.gessi.qrapids.app.domain.models;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "Alert")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "externalId")

    private String externalId;
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)

    private LocalDate date;
    @Column(name = "value")

    private float value;
    @Column(name = "threshold")

    private float threshold;
    @Column(name = "type")
    private AlertType type;
    @Column(name = "projectid")

    private String projectId;
    @Column(name = "affectedid")

    private String affectedId;

    public Alert (float value, float threshold, AlertType type, String projectId, String affectedId) {
        this.value = value;
        this.threshold = threshold;
        this.type = type;
        this.projectId = projectId;
        this.affectedId = affectedId;
        date = LocalDate.now();
        externalId  = UUID.randomUUID().toString();
    };

    public Alert() {
    }

    public Long getId(){
        return id;
    }
    public void setId (Long alertId) {
        id = alertId;
    }

    public String getExternalId(){
        return externalId;
    }
    public void setExternalId (String externalId) {
        this.externalId = externalId;
    }
    public LocalDate getDate(){
        return date;
    }
    public void setDate( LocalDate alertDate){
        date = alertDate;
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getAffectedId() {
        return affectedId;
    }

    public void setAffectedId(String affectedId) {
        this.affectedId = affectedId;
    }
}
