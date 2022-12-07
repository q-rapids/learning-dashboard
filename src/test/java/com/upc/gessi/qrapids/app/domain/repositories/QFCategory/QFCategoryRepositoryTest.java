package com.upc.gessi.qrapids.app.domain.repositories.QFCategory;

import com.upc.gessi.qrapids.app.domain.controllers.FactorsController;
import com.upc.gessi.qrapids.app.domain.models.Factor;
import com.upc.gessi.qrapids.app.domain.models.QFCategory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(SpringRunner.class)
@DataJpaTest
public class QFCategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QFCategoryRepository qfCategoryRepository;
    @Mock
    private FactorsController qualityFactorsDomainController;

    @Test
    public void findAllByOrderByUpperThresholdAsc() {
        // Given
        QFCategory qfCategoryBad = new QFCategory("Default", "#ff0000", 0.33f, "Bad");
        entityManager.persist(qfCategoryBad);

        QFCategory qfCategoryGood = new QFCategory("Default", "#00ff00", 1f,"Good");
        entityManager.persist(qfCategoryGood);

        QFCategory qfCategoryNeutral = new QFCategory("Default", "#ff8000", 0.67f,"Neutral");
        entityManager.persist(qfCategoryNeutral);

        // When
        List<QFCategory> categoryList = qfCategoryRepository.findAllByOrderByUpperThresholdAsc();

        // Then
        assertEquals(qfCategoryBad, categoryList.get(0));
        assertEquals(qfCategoryNeutral, categoryList.get(1));
        assertEquals(qfCategoryGood, categoryList.get(2));

    }

    @Test
    public void findAllByCategoryName() {
        // Given
        QFCategory qfCategoryDefaultBad = new QFCategory("Default", "#ff0000", 0.33f, "Bad");
        entityManager.persist(qfCategoryDefaultBad);

        QFCategory qfCategoryDefaultGood = new QFCategory("Default", "#00ff00", 1f,"Good");
        entityManager.persist(qfCategoryDefaultGood);

        QFCategory qfCategoryDefaultNeutral = new QFCategory("Default", "#ff8000", 0.67f,"Neutral");
        entityManager.persist(qfCategoryDefaultNeutral);

        QFCategory qfCategory6memUp = new QFCategory("6 members contribution", "#ff0000", 1f, "Up");
        entityManager.persist(qfCategory6memUp);

        QFCategory qfCategory6memHigh = new QFCategory("6 members contribution", "#00ff00", 0.8f,"High");
        entityManager.persist(qfCategory6memHigh);

        QFCategory qfCategory6memGood = new QFCategory("6 members contribution", "#00ff00", 0.5f,"Neutral");
        entityManager.persist(qfCategory6memGood);

        QFCategory qfCategory6memLow = new QFCategory("6 members contribution", "#ff8000", 0.3f,"Low");
        entityManager.persistAndFlush(qfCategory6memLow);

        QFCategory qfCategory6memDown = new QFCategory("6 members contribution", "#ff8000", 0.15f,"Down");
        entityManager.persistAndFlush(qfCategory6memDown);


        // When
        List<QFCategory> categoryList = qfCategoryRepository.findAllByName("6 members contribution");

        //System.out.println(categoryList.get(0).getUpperThreshold());
        //System.out.println(categoryList.get(0).getName());
        //System.out.println(categoryList.get(0).getType());

        // Then
        assertEquals(qfCategory6memLow.getName(), categoryList.get(0).getName());
        assertEquals(5, categoryList.size());

    }

    @Test
    public void findAllByCategoryType() {

        QFCategory qfCategoryDefaultBad = new QFCategory("Default", "#ff0000", 0.33f, "Bad");
        entityManager.persist(qfCategoryDefaultBad);

        QFCategory qfCategoryDefaultGood = new QFCategory("Default", "#00ff00", 1f,"Good");
        entityManager.persist(qfCategoryDefaultGood);

        QFCategory qfCategoryDefaultNeutral = new QFCategory("Default", "#ff8000", 0.67f,"Neutral");
        entityManager.persist(qfCategoryDefaultNeutral);

        QFCategory qfCategory6memUp = new QFCategory("6 members contribution", "#ff0000", 1f, "Up");
        entityManager.persist(qfCategory6memUp);

        QFCategory qfCategory6memHigh = new QFCategory("6 members contribution", "#00ff00", 0.8f,"High");
        entityManager.persist(qfCategory6memHigh);

        QFCategory qfCategory6memGood = new QFCategory("6 members contribution", "#00ff00", 0.5f,"Neutral");
        entityManager.persist(qfCategory6memGood);

        QFCategory qfCategory6memLow = new QFCategory("6 members contribution", "#ff8000", 0.3f,"Low");
        entityManager.persist(qfCategory6memLow);

        QFCategory qfCategory6memDown = new QFCategory("6 members contribution", "#ff8000", 0.15f,"Down");
        entityManager.persist(qfCategory6memDown);

        // When
        List<QFCategory> categoryList = qfCategoryRepository.findAllByType("Down");

        // Then
        assertEquals("Down", categoryList.get(0).getType());
        assertEquals("6 members contribution", categoryList.get(0).getName());

    }

}