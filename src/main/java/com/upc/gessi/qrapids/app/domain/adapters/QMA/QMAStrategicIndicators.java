package com.upc.gessi.qrapids.app.domain.adapters.QMA;

import DTOs.EstimationEvaluationDTO;
import DTOs.EvaluationDTO;
import DTOs.QuadrupletDTO;
import DTOs.StrategicIndicatorEvaluationDTO;
import com.upc.gessi.qrapids.app.config.QMAConnection;
import com.upc.gessi.qrapids.app.domain.controllers.StrategicIndicatorsController;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.Feedback;
import com.upc.gessi.qrapids.app.domain.models.Strategic_Indicator;
import com.upc.gessi.qrapids.app.domain.repositories.Feedback.FeedbackRepository;
import com.upc.gessi.qrapids.app.domain.repositories.Project.ProjectRepository;
import com.upc.gessi.qrapids.app.domain.repositories.SICategory.SICategoryRepository;
import com.upc.gessi.qrapids.app.domain.repositories.StrategicIndicator.StrategicIndicatorRepository;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOAssessment;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStrategicIndicatorEvaluation;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import evaluation.StrategicIndicator;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import util.Queries;


import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class QMAStrategicIndicators {

    @Autowired
    private QMAConnection qmacon;

    @Autowired
    private SICategoryRepository SICatRep;

    @Autowired
    private StrategicIndicatorRepository siRep;

    @Autowired
    private ProjectRepository prjRep;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private StrategicIndicatorsController strategicIndicatorsController;

    public boolean prepareSIIndex(String projectID) throws IOException {
        qmacon.initConnexion();
        return Queries.prepareSIIndex(projectID);
    }

    public List<DTOStrategicIndicatorEvaluation> CurrentEvaluation(String prj, String profile) throws IOException, CategoriesException, ProjectNotFoundException {
        List<DTOStrategicIndicatorEvaluation> result;

        // Data coming from QMA API
        qmacon.initConnexion();
        List<StrategicIndicatorEvaluationDTO> evals = StrategicIndicator.getEvaluations(prj);
        //Connection.closeConnection();
        result = StrategicIndicatorEvaluationDTOListToDTOStrategicIndicatorEvaluationList(prj, profile, evals);

        return result;
    }

    public DTOStrategicIndicatorEvaluation SingleCurrentEvaluation(String prj, String profile, String strategicIndicatorId) throws IOException, CategoriesException, ProjectNotFoundException {
        qmacon.initConnexion();
        StrategicIndicatorEvaluationDTO strategicIndicatorEvaluationDTO = StrategicIndicator.getSingleEvaluation(prj, strategicIndicatorId);
        List<StrategicIndicatorEvaluationDTO> strategicIndicatorEvaluationDTOList = new ArrayList<>();
        strategicIndicatorEvaluationDTOList.add(strategicIndicatorEvaluationDTO);
        return StrategicIndicatorEvaluationDTOListToDTOStrategicIndicatorEvaluationList(prj, profile,
                strategicIndicatorEvaluationDTOList).get(0);
    }

    public List<DTOStrategicIndicatorEvaluation> HistoricalData(LocalDate from, LocalDate to, String prj, String profile) throws IOException, CategoriesException, ProjectNotFoundException {
        List<DTOStrategicIndicatorEvaluation> result;

        // Data coming from QMA API
        qmacon.initConnexion();
        //using dates from 1/1/2015 to now at the moment
        List<StrategicIndicatorEvaluationDTO> evals = StrategicIndicator.getEvaluations(prj, from, to);
        //Connection.closeConnection();
        result = StrategicIndicatorEvaluationDTOListToDTOStrategicIndicatorEvaluationList(prj, profile, evals);

        return result;
    }

    public boolean isCategoriesEmpty() {
     if (SICatRep.count() == 0)
         return true;
     else
         return false;
    }

    public boolean setStrategicIndicatorValue(String prj,
                                              String strategicIndicatorID,
                                              String strategicIndicatorName,
                                              String strategicIndicatorDescription,
                                              Float value,
                                              String info,
                                              LocalDate date,
                                              List<DTOAssessment> assessment,
                                              List<String> missingFactors,
                                              long dates_mismatch
                                              ) throws IOException {

        RestStatus status;
        if (assessment == null) {
            status = StrategicIndicator.setStrategicIndicatorEvaluation(prj,
                                                            strategicIndicatorID,
                                                            strategicIndicatorName,
                                                            strategicIndicatorDescription,
                                                            value,
                                                            info,
                                                            date,
                                                            null,
                                                            missingFactors,
                                                            dates_mismatch)
                    .status();
        } else {
            status = StrategicIndicator.setStrategicIndicatorEvaluation(prj,
                                                            strategicIndicatorID,
                                                            strategicIndicatorName,
                                                            strategicIndicatorDescription,
                                                            value,
                                                            info,
                                                            date,
                                                            listDTOSIAssessmentToEstimationEvaluationDTO(assessment),
                                                            missingFactors,
                                                            dates_mismatch)
                    .status();
        }
        return status.equals(RestStatus.OK) || status.equals(RestStatus.CREATED);
    }

    private List<DTOStrategicIndicatorEvaluation> StrategicIndicatorEvaluationDTOListToDTOStrategicIndicatorEvaluationList(String prjExternalId, String profileId, List<StrategicIndicatorEvaluationDTO> evals) throws CategoriesException, ProjectNotFoundException {
        List<DTOStrategicIndicatorEvaluation> si = new ArrayList<>();
        Long id = null;
        boolean hasBN = false;
        boolean hasFeedback = false;
        boolean found=false; // to check if the SI is in the database
        // get strategic indicators from DB by project and by profile (if specified)
        Iterable<Strategic_Indicator> sis_DB = strategicIndicatorsController.
                getStrategicIndicatorsByProjectAndProfile(prjExternalId,profileId);

        // si contains the list of evaluations for strategic indicators
        for (Iterator<StrategicIndicatorEvaluationDTO> iterSI = evals.iterator(); iterSI.hasNext(); ) {
            // For each SI
            StrategicIndicatorEvaluationDTO element = iterSI.next();
            id = null;
            hasBN = false;
            hasFeedback = false;
            found=false;
            for (Strategic_Indicator dbsi : sis_DB) {
                if (dbsi.getExternalId().equals(element.getID())) {
                    found = true;
                    id = dbsi.getId();
                    hasBN = dbsi.getNetwork() != null;
                    List<Feedback> feedback = new ArrayList<>();
                    try {
                        feedback = feedbackRepository.findAllBySiId(id);
                    } catch (Exception e) {
                    }
                    if (!feedback.isEmpty()) hasFeedback = true;
                }
            }
            // only return Strategic Indicator if it is in local database
            if (found) {
                //get categories
                List<DTOAssessment> categories = strategicIndicatorsController.getCategories();

                //bool that determines if the current SI has the estimation parameter
                if (element.getEstimation() == null || element.getEstimation().size() != element.getEvaluations().size())
                    throw new CategoriesException();

                buildDTOStrategicIndicatorEvaluationList(si, element, id, hasBN, hasFeedback, categories);
            }
        }
        return si;
    }

    private void buildDTOStrategicIndicatorEvaluationList(List<DTOStrategicIndicatorEvaluation> si, StrategicIndicatorEvaluationDTO element, Long id, boolean hasBN, boolean hasFeedback, List<DTOAssessment> categories) throws CategoriesException {
        Iterator<EstimationEvaluationDTO> iterSIEst = element.getEstimation().iterator();

        for (EvaluationDTO evaluation : element.getEvaluations()) {
            EstimationEvaluationDTO estimation = iterSIEst.next();
            //merge categories and estimation
            boolean hasEstimation = true;
            if (estimation == null || estimation.getEstimation() == null || estimation.getEstimation().size() == 0)
                hasEstimation = false;

            if (hasEstimation && estimation.getEstimation() != null && estimation.getEstimation().size() == categories.size()) {
                setValueAndThresholdForCategories(categories, estimation);
            } else if (hasEstimation) throw new CategoriesException();
            //calculate "fake" value if the SI has estimation
            if (hasEstimation) {
                buildDTOStrategicIndicatorEvaluationWithEstimation(si, element, id, hasBN, hasFeedback, categories, evaluation);
            } else {
                buildDTOStrategicIndicatorEvaluationWithoutEstimation(si, element, id, hasBN, hasFeedback, categories, evaluation, evaluation.getValue());
            }
        }
    }

    private void buildDTOStrategicIndicatorEvaluationWithoutEstimation(List<DTOStrategicIndicatorEvaluation> si, StrategicIndicatorEvaluationDTO element, Long id, boolean hasBN, boolean hasFeedback, List<DTOAssessment> categories, EvaluationDTO evaluation, Float value) {
        DTOStrategicIndicatorEvaluation dtoStrategicIndicatorEvaluation = new DTOStrategicIndicatorEvaluation(element.getID(),
                element.getName(),
                element.getDescription(),
                Pair.of(value, strategicIndicatorsController.getLabel(value)),
                evaluation.getRationale(),
                new ArrayList<>(categories),
                evaluation.getEvaluationDate(),
                evaluation.getDatasource(),
                id,
                categories.toString(),
                hasBN);
        dtoStrategicIndicatorEvaluation.setHasFeedback(hasFeedback);
        dtoStrategicIndicatorEvaluation.setMismatchDays(evaluation.getMismatchDays());
        dtoStrategicIndicatorEvaluation.setMissingFactors(evaluation.getMissingElements());
        si.add(dtoStrategicIndicatorEvaluation);
    }

    private void buildDTOStrategicIndicatorEvaluationWithEstimation(List<DTOStrategicIndicatorEvaluation> si, StrategicIndicatorEvaluationDTO element, Long id, boolean hasBN, boolean hasFeedback, List<DTOAssessment> categories, EvaluationDTO evaluation) {
        Float value = strategicIndicatorsController.getValueAndLabelFromCategories(categories).getFirst();
        buildDTOStrategicIndicatorEvaluationWithoutEstimation(si, element, id, hasBN, hasFeedback, categories, evaluation, value);
    }

    private void setValueAndThresholdForCategories(List<DTOAssessment> categories, EstimationEvaluationDTO estimation) throws CategoriesException {
        int changed = 0;
        int i = 0;
        for (DTOAssessment d : categories) {
            if (d.getLabel().equals(estimation.getEstimation().get(i).getSecond())) {
                d.setValue(estimation.getEstimation().get(i).getThird());
                d.setUpperThreshold(estimation.getEstimation().get(i).getFourth());
                ++changed;
            }
            ++i;
        }
        if (changed != categories.size())
            throw new CategoriesException();
    }

    private EstimationEvaluationDTO listDTOSIAssessmentToEstimationEvaluationDTO(List<DTOAssessment> assessment) {
        List<QuadrupletDTO<Integer, String, Float, Float>> estimation = new ArrayList<>();
        for (DTOAssessment dsa : assessment) {
            estimation.add(new QuadrupletDTO<Integer, String, Float, Float>(dsa.getId() != null ? dsa.getId().intValue() : null, dsa.getLabel(), dsa.getValue(), dsa.getUpperThreshold()));
        }
        return new EstimationEvaluationDTO(estimation);
    }


}