package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOCategoryThreshold;

public class DTOMetricCategory extends DTOCategoryThreshold {

    private String type;
    public DTOMetricCategory(Long id, String name, String color, float upperThreshold, String type) {
        super(id, name, color, upperThreshold);
        this.type=type;
    }

    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type=type;
    }
}
