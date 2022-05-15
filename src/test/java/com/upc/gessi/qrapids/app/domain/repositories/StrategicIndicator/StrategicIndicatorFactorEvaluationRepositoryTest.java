package com.upc.gessi.qrapids.app.domain.repositories.StrategicIndicator;

import com.upc.gessi.qrapids.app.domain.models.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DataJpaTest
public class StrategicIndicatorFactorEvaluationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StrategicIndicatorQualityFactorsRepository strategicIndicatorQualityFactorsRepository;

    @Test
    public void findByStrategic_indicatorId() {
        // Given
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        Project project = new Project(projectExternalId, projectName, projectDescription, null, true, "testURL1", "testURL2",false);
        entityManager.persist(project);

        String strategicIndicator1Name = "Product Quality";
        String strategicIndicator1Description = "Quality of the product built";
        Strategic_Indicator strategicIndicator1 = new Strategic_Indicator(strategicIndicator1Name, strategicIndicator1Description, null, project);
        entityManager.persist(strategicIndicator1);
        List<StrategicIndicatorQualityFactors> qualityFactors1 = new ArrayList<>();

        // define factor1 with its metric composition
        List<QualityFactorMetrics> qualityMetrics1 = new ArrayList<>();
        Metric metric1 = new Metric("duplication","Duplication", "Density of non-duplicated code",project, "Duplication category");
        metric1.setId(1L);
        entityManager.merge(metric1);
        Factor factor1 =  new Factor("codequality", "Quality of the implemented code", project, "testType");
        factor1.setId(1L);
        QualityFactorMetrics qfm1 = new QualityFactorMetrics(-1f, metric1, factor1);
        qfm1.setId(1L);
        qualityMetrics1.add(qfm1);
        factor1.setQualityFactorMetricsList(qualityMetrics1);
        factor1.setWeighted(false);
        entityManager.merge(factor1);
        entityManager.merge(qfm1);
        // define si with factor1 union
        Long siqf1Id = 1L;
        StrategicIndicatorQualityFactors siqf1 = new StrategicIndicatorQualityFactors(factor1, -1, strategicIndicator1);
        siqf1.setId(siqf1Id);
        entityManager.merge(siqf1);
        qualityFactors1.add(siqf1);

        // define factor2 with its metric composition
        List<QualityFactorMetrics> qualityMetrics2 = new ArrayList<>();
        Metric metric2 = new Metric("bugdensity","Bugdensity", "Density of files without bugs", project, "Bugdensity category");
        metric2.setId(2L);
        entityManager.merge(metric2);
        Factor factor2 =  new Factor("softwarestability", "Stability of the software under development", project, "testType");
        factor2.setId(2L);
        QualityFactorMetrics qfm2 = new QualityFactorMetrics(-1f, metric2, factor2);
        qfm2.setId(2L);
        qualityMetrics2.add(qfm2);
        factor2.setQualityFactorMetricsList(qualityMetrics2);
        factor2.setWeighted(false);
        entityManager.merge(factor2);
        entityManager.merge(qfm2);
        // define si with factor2 union
        Long siqf2Id = 2L;
        StrategicIndicatorQualityFactors siqf2 = new StrategicIndicatorQualityFactors( factor2, -1, strategicIndicator1);
        siqf2.setId(siqf2Id);
        entityManager.merge(siqf2);
        qualityFactors1.add(siqf2);

        // define factor3 with its metric composition
        List<QualityFactorMetrics> qualityMetrics3 = new ArrayList<>();
        Metric metric3 = new Metric("fasttests","Fast Tests", "Percentage of tests under the testing duration threshold",project, "Fast Test category");
        metric3.setId(3L);
        entityManager.merge(metric3);
        Factor factor3 =  new Factor("testingstatus", "Performance of testing phases", project, "testType");
        factor3.setId(3L);
        QualityFactorMetrics qfm3 = new QualityFactorMetrics(-1f, metric3, factor3);
        qfm3.setId(3L);
        qualityMetrics3.add(qfm3);
        factor3.setQualityFactorMetricsList(qualityMetrics3);
        factor3.setWeighted(false);
        entityManager.merge(factor3);
        entityManager.merge(qfm3);
        // define si with factor3 union
        Long siqf3Id = 3L;
        StrategicIndicatorQualityFactors siqf3 = new StrategicIndicatorQualityFactors( factor3, -1, strategicIndicator1);
        siqf3.setId(siqf3Id);
        entityManager.merge(siqf3);
        qualityFactors1.add(siqf3);

        // finish define si with its factors composition
        strategicIndicator1.setStrategicIndicatorQualityFactorsList(qualityFactors1);
        strategicIndicator1.setWeighted(false);
        entityManager.merge(strategicIndicator1);


        String strategicIndicator2Name = "Blocking";
        String strategicIndicator2Description = "Blocking elements";
        Strategic_Indicator strategicIndicator2 = new Strategic_Indicator(strategicIndicator2Name, strategicIndicator2Description, null, project);
        entityManager.persist(strategicIndicator2);
        List<StrategicIndicatorQualityFactors> qualityFactors2 = new ArrayList<>();

        // define factor4 with its metric composition
        List<QualityFactorMetrics> qualityMetrics4 = new ArrayList<>();
        Metric metric4 = new Metric("nonblockingfiles","Non-blocking Files", "Density of non-blocking source files",project, "Non-blocking Files category");
        metric4.setId(4L);
        entityManager.merge(metric4);
        Factor factor4 =  new Factor("blockingcode", " \tDensity of blocking code", project, "testType");
        factor4.setId(4L);
        QualityFactorMetrics qfm4 = new QualityFactorMetrics(-1f, metric4, factor4);
        qfm4.setId(4L);
        qualityMetrics4.add(qfm4);
        factor4.setQualityFactorMetricsList(qualityMetrics4);
        factor4.setWeighted(false);
        entityManager.merge(factor4);
        entityManager.merge(qfm4);
        // define si with factor4 union
        Long siqf4Id = 4L;
        StrategicIndicatorQualityFactors siqf4 = new StrategicIndicatorQualityFactors(factor4, -1, strategicIndicator2);
        siqf4.setId(siqf4Id);
        entityManager.merge(siqf4);
        qualityFactors2.add(siqf4);

        // define factor5 with its metric composition
        List<QualityFactorMetrics> qualityMetrics5 = new ArrayList<>();
        Metric metric5 = new Metric("fasttests","Fast Tests", "Percentage of tests under the testing duration threshold",project, "Fast Test category");
        metric5.setId(5L);
        entityManager.merge(metric5);
        Factor factor5 =  new Factor("testingstatus", "Performance of testing phases", project, "testType");
        factor5.setId(5L);
        QualityFactorMetrics qfm5 = new QualityFactorMetrics(-1f, metric5, factor5);
        qfm5.setId(5L);
        qualityMetrics5.add(qfm5);
        factor5.setQualityFactorMetricsList(qualityMetrics5);
        factor5.setWeighted(false);
        entityManager.merge(factor5);
        entityManager.merge(qfm5);
        // define si with factor5 union
        Long siqf5Id = 5L;
        StrategicIndicatorQualityFactors siqf5 = new StrategicIndicatorQualityFactors(factor5, -1, strategicIndicator2);
        siqf5.setId(siqf5Id);
        entityManager.merge(siqf5);
        qualityFactors2.add(siqf5);

        // define factor6 with its metric composition
        List<QualityFactorMetrics> qualityMetrics6 = new ArrayList<>();
        Metric metric6 = new Metric("bugdensity","Bugdensity", "Density of files without bugs", project, "Bugdensity category");
        metric6.setId(6L);
        entityManager.merge(metric6);
        Factor factor6 =  new Factor("qualityissuespecification", "Percentage of issues planned for the next release that are completely specified and ready to be developed", project, "testType");
        factor6.setId(6L);
        QualityFactorMetrics qfm6 = new QualityFactorMetrics(-1f, metric6, factor6);
        qfm6.setId(6L);
        qualityMetrics6.add(qfm6);
        factor6.setQualityFactorMetricsList(qualityMetrics6);
        factor6.setWeighted(false);
        entityManager.merge(factor6);
        entityManager.merge(qfm6);
        // define si with factor6 union
        Long siqf6Id = 6L;
        StrategicIndicatorQualityFactors siqf6 = new StrategicIndicatorQualityFactors( factor6, -1, strategicIndicator2);
        siqf6.setId(siqf6Id);
        entityManager.merge(siqf6);
        qualityFactors2.add(siqf6);

        // finish define si with its factors composition
        strategicIndicator2.setStrategicIndicatorQualityFactorsList(qualityFactors2);
        strategicIndicator2.setWeighted(false);
        //entityManager.persistAndFlush(strategicIndicator2);
        entityManager.merge(strategicIndicator2);
        entityManager.flush();

        // When
        List<StrategicIndicatorQualityFactors> strategicIndicatorQualityFactorsList = strategicIndicatorQualityFactorsRepository.findByStrategic_indicator(strategicIndicator1);
        List<String> strategicIndicatorQualityFactorsListFound = convertToString(strategicIndicatorQualityFactorsList);

        // Then
        int expectedNumberStrategicIndicatorsQualityFactorsFound = 3;
        assertEquals(expectedNumberStrategicIndicatorsQualityFactorsFound, strategicIndicatorQualityFactorsList.size());
        assertEquals(strategicIndicator1.getWeightsWithExternalId(), strategicIndicatorQualityFactorsListFound);
    }

    private List<String> convertToString (List<StrategicIndicatorQualityFactors> strategicIndicatorQualityFactorsList) {
        List <String> siqfList = new ArrayList<>();
        for (StrategicIndicatorQualityFactors s : strategicIndicatorQualityFactorsList) {
            siqfList.add(s.getFactor().getExternalId());
            siqfList.add(String.valueOf(s.getWeight()));
        }
        return siqfList;
    }

}
