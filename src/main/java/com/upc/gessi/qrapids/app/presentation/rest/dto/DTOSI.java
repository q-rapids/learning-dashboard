package com.upc.gessi.qrapids.app.presentation.rest.dto;

import java.util.List;

public class DTOSI { // get strategic indicator information from DB
    private Long id;
    private String externalId;
    private String name;
    private String description;
    private Float threshold;
    private byte[] network;
    private List<String> qualityFactors;
    private boolean weighted;
    private List<String> qualityFactorsWeights;

    public DTOSI(Long id, String externalId, String name, String description, Float threshold, byte[] network, List<String> qualityFactors, boolean weighted, List<String> qualityFactorsWeights) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.threshold = threshold;
        this.network = network;
        this.qualityFactors = qualityFactors;
        this.weighted = weighted;
        this.qualityFactorsWeights = qualityFactorsWeights;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getThreshold() {
        return threshold;
    }

    public void setThreshold(Float threshold) {
        this.threshold = threshold;
    }

    public byte[] getNetwork() {
        return network;
    }

    public void setNetwork(byte[] network) {
        this.network = network;
    }

    public List<String> getQualityFactors() {
        return qualityFactors;
    }

    public void setQualityFactors(List<String> qualityFactors) {
        this.qualityFactors = qualityFactors;
    }

    public void setWeighted(boolean weighted) { this.weighted = weighted; }

    public boolean isWeighted() { return weighted; }

    public List<String> getQualityFactorsWeights() { return qualityFactorsWeights; }
}
