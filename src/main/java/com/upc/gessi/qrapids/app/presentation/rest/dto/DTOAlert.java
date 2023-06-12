package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.models.AlertStatus;
import com.upc.gessi.qrapids.app.domain.models.AlertType;

import java.sql.Date;

public class DTOAlert {

    private Long id;
    private AlertType type;
    private float value;
    private Float threshold;
    private Date date;
    private AlertStatus status;
    private String affectedId;
    private String affectedType;
    private Date predictionDate;
    private String predictionTechnique;


    public DTOAlert(Long id, String affectedId, String affectedType, AlertType type, float value, Float threshold, Date date, AlertStatus status, Date predictionDate, String technique) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.threshold = threshold;
        this.date = date;
        this.status = status;
        this.affectedId = affectedId;
        this.affectedType =affectedType;
        this.predictionDate = predictionDate;
        this.predictionTechnique = technique;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAffectedId() {
        return affectedId;
    }

    public void setAffectedId(String affectedId) {
        this.affectedId = affectedId;
    }

    public String getAffectedType() {
        return affectedType;
    }

    public void setAffectedType(String affectedType) {
        this.affectedType = affectedType;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public Date getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(Date predictionDate) {
        this.predictionDate = predictionDate;
    }

    public String getPredictionTechnique() {
        return predictionTechnique;
    }

    public void setPredictionTechnique(String technique) {
        this.predictionTechnique = technique;
    }
}
