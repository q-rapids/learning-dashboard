package com.upc.gessi.qrapids.app.presentation.rest.dto;

import com.upc.gessi.qrapids.app.domain.controllers.StrategicIndicatorsController;
import org.springframework.data.util.Pair;

import java.time.LocalDate;
import java.util.List;

/**
 * This class define objects with all the information about a Strategic Indicator, which includes the Factor id, name and a
 * set of Metrics
 *
 * @author Oriol M./Guillem B./Lidia L.
 */
public class DTOStrategicIndicatorEvaluation {
    //class attributes
    private String id;
    private Long dbId;
    private String name;
    private String description;
    private Pair<Float, String> value;
    private String value_description;
    private String rationale;
    private List<DTOAssessment> probabilities;
    private LocalDate date;
    private String datasource;
    private String categories_description;
    private boolean hasBN;
    private boolean hasFeedback;
    private Pair<Float, Float> confidence80;
    private Pair<Float, Float> confidence95;
    private String forecastingError;
    private int mismatchDays;
    private List<String> missingFactors;

    /**
     * Constructor of the DTO of Strategic Indicators Evaluation
     *
     * @param id The parameter defines the ID of the Strategic Indicator
     * @param name The parameter defines the name of the Strategic Indicator
     * @param description The parameter defines the description of the Strategic Indicator
     * @param probabilities The parameter defines the target of the KPI
     * @param value The parameter defines the value of the Strategic Indicator evaluation
     * @param date The parameter defines the date of the Strategic Indicator evaluation
     * @param datasource The parameter defines the data source of the Strategic Indicator evaluation
     * @param dbId The parameter defines the database id of the Strategic Indicator
     * @param categories The parameter include the list of categories associated to the strategic indicator
     */
    public DTOStrategicIndicatorEvaluation(String id, String name, String description, Pair<Float, String> value, String rationale, List<DTOAssessment> probabilities, LocalDate date, String datasource, Long dbId, String categories, boolean hasBN) {
        setId(id);
        setName(name);
        setDescription(description);
        setValue(value);
        setRationale(rationale);
        setProbabilities(probabilities);
        setDate(date);
        setDbId(dbId);
        setDatasource(datasource);
        setCategories_description(categories);
        setHasBN(hasBN);
    }

    public DTOStrategicIndicatorEvaluation(String id, String name, String description, Pair<Float, String> value, Pair<Float, Float> confidence80, Pair<Float, Float> confidence95, String rationale, List<DTOAssessment> probabilities, LocalDate date, String datasource, Long dbId, String categories, boolean hasBN) {
        setId(id);
        setName(name);
        setDescription(description);
        setValue(value);
        setConfidence80(confidence80);
        setConfidence95(confidence95);
        setRationale(rationale);
        setProbabilities(probabilities);
        setDate(date);
        setDbId(dbId);
        setDatasource(datasource);
        setCategories_description(categories);
        setHasBN(hasBN);
    }

    public DTOStrategicIndicatorEvaluation(String id, String name, String forecastingError) {
        this.id = id;
        this.name = name;
        this.forecastingError = forecastingError;
    }

    /**
     * All the getters from the class DTOStrategicIndicatorsEvaluation that returns the value of the attribute defined
     * in the header of the getter
     *
     * @return the value of the attribute defined in the header of the getter
     */

    /**
     * All the setters from the class DTODetailedStrategicIndicators that set the value of the respective attribute
     * passed as parameter in the value of the class attribute
     *
     */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name.isEmpty() ? this.id : this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null)
            this.description = description;
    }

    public Pair<Float, String> getValue() {
        return value;
    }

    public String getValue_description() { return value_description;}

    private void setValue_description(Pair<Float, String> value) {
        this.value_description = StrategicIndicatorsController.buildDescriptiveLabelAndValue(value);
    }

    public void setValue(Pair<Float, String> value) {
        this.value = value;
        setValue_description(value);
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getRationale() {
        return rationale;
    }

    public List<DTOAssessment> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(List<DTOAssessment> probabilities) {
        this.probabilities = probabilities;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {

        if (datasource !=null )
            this.datasource = datasource;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getCategories_description() {
        return categories_description;
    }

    public void setCategories_description(String categories_description) {
        if (categories_description!=null)
            this.categories_description = categories_description;
    }

    public boolean isHasBN() {
        return hasBN;
    }

    public void setHasBN(boolean hasBN) {
        this.hasBN = hasBN;
    }

    public boolean isHasFeedback() {
        return hasFeedback;
    }

    public void setHasFeedback(boolean hasFeedback) {
        this.hasFeedback = hasFeedback;
    }

    public String getForecastingError() {
        return forecastingError;
    }

    public void setForecastingError(String forecastingError) {
        this.forecastingError = forecastingError;
    }

    public int getMismatchDays() {
        return mismatchDays;
    }

    public void setMismatchDays(int mismatchDays) {
        this.mismatchDays = mismatchDays;
    }

    public List<String> getMissingFactors() {
        return missingFactors;
    }

    public void setMissingFactors(List<String> missingFactors) {
        this.missingFactors = missingFactors;
    }

    public void setConfidence80(Pair<Float, Float> confidence80) {
        this.confidence80 = confidence80;
    }

    public Pair<Float, Float> getConfidence80() {
        return confidence80;
    }

    public void setConfidence95(Pair<Float, Float> confidence95) {
        this.confidence95 = confidence95;
    }

    public Pair<Float, Float> getConfidence95() {
        return confidence95;
    }
}
