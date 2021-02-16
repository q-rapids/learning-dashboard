package com.upc.gessi.qrapids.app.domain.adapters;

import com.google.gson.Gson;
import com.upc.gessi.qrapids.app.domain.controllers.StrategicIndicatorsController;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOAssessment;
import com.upc.gessi.qrapids.app.presentation.rest.dto.assessmentSI.DTOAssessmentSI;
import com.upc.gessi.qrapids.app.presentation.rest.dto.assessmentSI.DTOCategorySI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssesSI {

    @Autowired
    private StrategicIndicatorsController strategicIndicatorsController;

    @Value("${assessSI.url:}")
    private String url;

    public List<DTOAssessment> assesSI(String siId, Map<String, String> mapFactors, File network) {

        mapFactors = new LinkedHashMap<>(mapFactors);

        try {
            RestTemplate restTemplate = new RestTemplate();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url + "/api/si/assessment");

            MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
            params.add("SIid", siId);
            List<String> factorNames = new ArrayList<>(mapFactors.keySet());
            for (String name : factorNames) {
                params.add("factorNames", name);
            }
            List<String> factorValues = new ArrayList<>(mapFactors.values());
            for (String value: factorValues) {
                params.add("factorValues", value);
            }
            params.add("network", new FileSystemResource(network));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(params, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(builder.build().encode().toUri(), requestEntity, String.class);

            HttpStatus statusCode = responseEntity.getStatusCode();
            List<DTOAssessment> dtoSiAssessment;
            if (statusCode == HttpStatus.OK) {
                Gson gson = new Gson();
                DTOAssessmentSI assessmentSI = gson.fromJson(responseEntity.getBody(), DTOAssessmentSI.class);
                dtoSiAssessment = dtoAssessmentSItoDTOSIAssessment(assessmentSI.getProbsSICategories());
            }
            else {
                dtoSiAssessment = new ArrayList<>();
            }
            return dtoSiAssessment;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // If there is no BN, the assessment is the factors average
    public float assesSI(List<Float> factors_assessment, int n_factors) {
        try {
            float total = 0.f;
            float result =0.f;

            for (Float factor : factors_assessment) {
                total += factor;
            }
            if (total>0)
                result = total/n_factors;

            return result;

        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(AssesSI.class);
            logger.error(e.getMessage(), e);
            return 0.f;
        }
    }

    public List<DTOAssessment> dtoAssessmentSItoDTOSIAssessment(List<DTOCategorySI> catsEstimation) {
        List<DTOAssessment> categories = strategicIndicatorsController.getCategories();
        if (catsEstimation.size() == categories.size()) {
            int i = 0;
            for (DTOAssessment assessment : categories) {
                if (assessment.getLabel().equals(catsEstimation.get(catsEstimation.size() - 1 - i).getIdSICategory())) {
                    assessment.setValue(catsEstimation.get(catsEstimation.size() - 1 - i).getProbSICategory());
                }
                ++i;
            }
        }
        return categories;
    }

    public float assesSI_weighted(List<Float> factorsAssessment, List<Float> weights) {
        try {
            float total = 0.f;
            float result =0.f;

            for (int i = 0; i < factorsAssessment.size(); i++) {
                total += ((weights.get(i)/100)*factorsAssessment.get(i));
            }
            if (total>0)
                result = total/1; // sum of weights always is 1 = 100%

            return result;

        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(AssesSI.class);
            logger.error(e.getMessage(), e);
            return 0.f;
        }
    }
}
