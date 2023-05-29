package com.upc.gessi.qrapids.app.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataSource {

    GITHUB("GITHUB"),
    TAIGA("TAIGA"),
    PRT("PRT");

    private String dataSourceType;

    DataSource(String dataSourceType){
        this.dataSourceType = dataSourceType;
    }

    @JsonCreator
    public static DataSource fromString(String dataSourceType){
        return dataSourceType == null ? null : DataSource.valueOf(dataSourceType.toUpperCase());
    }

    @JsonValue
    public String getDataSourceType() {
        return this.dataSourceType.toUpperCase();
    }
}
