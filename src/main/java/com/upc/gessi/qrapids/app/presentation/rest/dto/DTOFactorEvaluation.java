package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.controllers.FactorsController;
import org.springframework.data.util.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * This class define objects with all the information about a Factor, which includes the Factor id, name, evaluationdate
 * and evaluationValue
 *
 * @author Oriol M./Guillem B/Lidia L.
 */

public class DTOFactorEvaluation {

    //class attributes
    private String id;
    private String name;
    private String description;
    private Pair<Float, String> value;
    private String value_description;
    private LocalDate date;
    private String datasource;
    private String rationale;
    private List<String> strategic_indicators;
    private Pair<Float, Float> confidence80;
    private Pair<Float, Float> confidence95;
    private String forecastingError;
    private int mismatchDays;
    private List<String> missingMetrics;
    private String type;

    /**
     * Constructor of the DTO of Factors
     *
     * @param id The parameter defines the ID of the Factor
     * @param name The parameter defines the name of the Factor
     * @param description The parameter defines the description of the Factor
     * @param value The parameter defines the value of the factor evaluation
     * @param date The parameter defines the date of the factor evaluation
     * @param datasource The parameter defines the data source of the factor evaluation
     * @param rationale The parameter describes textually the rationale behind the value
     * @param strategicIndicators The list of strategic indicators IDs using this factor evaluation
     */
    public DTOFactorEvaluation(String id, String name, String description, Pair<Float, String> value, LocalDate date, String datasource,
                               String rationale, List<String> strategicIndicators) {
        setId(id);
        setName(name);
        setDescription(description);
        setValue(value);
        setDate(date);
        setDatasource(datasource);
        setRationale(rationale);
        setStrategicIndicators(strategicIndicators);
    }

    /**
     * Constructor of the DTO of Factor
     *
     * @param id The parameter defines the ID of the Factor
     * @param name The parameter defines the name of the Factor
     * @param description The parameter defines the description of the Factor
     * @param value The parameter defines the value of the factor evaluation
     * @param date The parameter defines the date of the factor evaluation
     * @param datasource The parameter defines the datasource of the factor evaluation
     * @param rationale The parameter describes textually the rationale behind the value
     * @param confidence80 Upper and lower values respectively for the 80% confidence interval
     * @param confidence95 Upper and lower values respectively for the 95% confidence interval
     */
    public DTOFactorEvaluation(String id, String name, String description, String datasource, String rationale, LocalDate date, Pair<Float, String> value, Pair<Float, Float> confidence80, Pair<Float, Float> confidence95) {
        setId(id);
        setName(name);
        setDescription(description);
        setValue(value);
        setDate(date);
        setDatasource(datasource);
        setRationale(rationale);
        setConfidence80(confidence80);
        setConfidence95(confidence95);
    }

    public DTOFactorEvaluation(String id, String name, String forecastingError) {
        this.id = id;
        this.name = name;
        this.forecastingError = forecastingError;
    }

    /**
     * All the getters from the class DTOFactor that returns the value of the attribute defined
     * in the header of the getter
     *
     * @return the value of the attribute defined in the header of the getter
     */
    public String getId() {
        return id;
    }

    public String getName() {
        return this.name.isEmpty() ? this.id : this.name;
    }

    public Pair<Float, String> getValue() {
        return value;
    }

    public String getValue_description () { return value_description; }

    public LocalDate getDate() {
        return date;
    }

    public String getFormattedDate () {
        return getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getDatasource() {
        return datasource;
    }

    public String getRationale() { return rationale;}

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        if (description!=null)
            this.description = description;
    }
    public void setValue(Pair<Float, String> value) {
        this.value = value;
        setValue_description(value);
    }

    public String getDescription() {return this.description; }

    public void setValue_description(Pair<Float, String> value) {
        this.value_description = FactorsController.buildDescriptiveLabelAndValue(value);
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setDatasource(String datasource) {
        if (datasource!=null)
            this.datasource = datasource;
    }

    public void setRationale(String rationale) {
        if (rationale!=null)
            this.rationale = rationale;
   }

    public void setStrategicIndicators(List<String> strategicIndicators) {
        this.strategic_indicators = strategicIndicators;
    }

    public void addStrategicIndicator(String strategicIndicatorID) {
        this.strategic_indicators.add(strategicIndicatorID);
    }

    public List<String> getStrategicIndicators() {
        return strategic_indicators;
    }

    public String getForecastingError() {
        return forecastingError;
    }

    public void setForecastingError(String forecastingError) {
        this.forecastingError = forecastingError;
    }

    public Pair<Float, Float> getConfidence80() {
        return confidence80;
    }

    public void setConfidence80(Pair<Float, Float> confidence80) {
        this.confidence80 = confidence80;
    }

    public Pair<Float, Float> getConfidence95() {
        return confidence95;
    }

    public void setConfidence95(Pair<Float, Float> confidence95) {
        this.confidence95 = confidence95;
    }

    public int getMismatchDays() {
        return mismatchDays;
    }

    public void setMismatchDays(int mismatchDays) {
        this.mismatchDays = mismatchDays;
    }

    public List<String> getMissingMetrics() {
        return missingMetrics;
    }

    public void setMissingMetrics(List<String> missingMetrics) {
        this.missingMetrics = missingMetrics;
    }

    public void setType(String type) {this.type=type;}

    public String getType() {return type;}
}
