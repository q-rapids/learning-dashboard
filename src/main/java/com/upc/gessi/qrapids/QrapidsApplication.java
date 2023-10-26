package com.upc.gessi.qrapids;

import com.upc.gessi.qrapids.app.domain.controllers.*;
import com.upc.gessi.qrapids.app.domain.models.MetricCategory;
import com.upc.gessi.qrapids.app.domain.models.QFCategory;
import com.upc.gessi.qrapids.app.domain.models.SICategory;
import com.upc.gessi.qrapids.app.presentation.rest.services.Alerts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

@SpringBootApplication
@EnableScheduling
public class QrapidsApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(QrapidsApplication.class);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Value("${projects.dir:}") // default -> empty string
	private String projectsDir;

	static ConfigurableApplicationContext context;

	/*@Scheduled(cron = "${cron.expression:-}") // default -> disable scheduled task
	public void scheduleTask() throws ParseException, ProjectNotFoundException, IOException, CategoriesException {
		// ToDo: decide if we also copy this code to assessSI function
		Logger logger = LoggerFactory.getLogger(Alerts.class);
		logger.info("Start Scheduled task: " + new Timestamp(System.currentTimeMillis()));
		logger.info("projects dir: " + projectsDir);
		LocalDate evaluationLocalDate = LocalDate.now(); // we need LocalDate for assessStrategicIndicators
		Date evaluationDate= Date.from(evaluationLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()); // we need Date for evaluateQualityModel in qrapids-eval libs

		// params config:
		// 					projects dir path, evaluationDate, null
		//					projects dir path, fromDate, toDate
		Eval.evaluateQualityModel(projectsDir, evaluationDate, null);

		boolean correct = true;
		correct = context.getBean(FactorsController.class).assessQualityFactors(null, evaluationLocalDate);

		if (correct) {
			correct = context.getBean(StrategicIndicatorsController.class).assessStrategicIndicators(null, evaluationLocalDate);
		}

		if (!correct) { // check if the assessment complete with error
			logger.error(evaluationLocalDate + ": factors or strategic indicators assessment complete with error.");
		}
	}*/


	public static void main(String[] args) throws Exception {

		context = SpringApplication.run(QrapidsApplication.class, args);

		// Check the categories in the SQL database and if they are empty create the default ones
		List<SICategory> siCategoryList = context.getBean(StrategicIndicatorsController.class).getStrategicIndicatorCategories();
		List<QFCategory> factorCategoryList = context.getBean(FactorsController.class).getFactorCategories("Default");
		List<MetricCategory> metricCategoryList = context.getBean(MetricsController.class).getMetricCategories("Default");

		try {
			// Declare default categories
			List<Map<String, String>> categories = new ArrayList<>();
			Map<String,String> cat1 = new HashMap<>();
			cat1.put("color", "#00ff00");
			cat1.put("name", "Good");
			cat1.put("upperThreshold", "100");
			categories.add(cat1);
			Map<String,String> cat2 = new HashMap<>();
			cat2.put("color", "#ff8000");
			cat2.put("name", "Normal");
			cat2.put("upperThreshold", "67");
			categories.add(cat2);
			Map<String,String> cat3 = new HashMap<>();
			cat3.put("color", "#ff0000");
			cat3.put("name", "Bad");
			cat3.put("upperThreshold", "33");
			categories.add(cat3);

			// Save Strategic Indicator categories
			if (siCategoryList.size() == 0) {
				context.getBean(StrategicIndicatorsController.class).newStrategicIndicatorCategories(categories);
			}
			// Declare and save Metric categories with new attributes

			categories = new ArrayList<>();
			cat1 = new HashMap<>();
			cat1.put("color", "#00ff00");
			cat1.put("type", "Good");
			cat1.put("name", "Default");
			cat1.put("upperThreshold", "100");
			categories.add(cat1);
			cat2 = new HashMap<>();
			cat2.put("color", "#ff8000");
			cat2.put("type", "Normal");
			cat2.put("name", "Default");
			cat2.put("upperThreshold", "67");
			categories.add(cat2);
			cat3 = new HashMap<>();
			cat3.put("color", "#ff0000");
			cat3.put("type", "Bad");
			cat3.put("name", "Default");
			cat3.put("upperThreshold", "33");
			categories.add(cat3);

			if (metricCategoryList.size() == 0) {
				context.getBean(MetricsController.class).newMetricCategories(categories, "Default");
			}
			// Save Factor categories
			if (factorCategoryList.size() == 0){
				context.getBean(FactorsController.class).newFactorCategories(categories, "Default");
			}
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(Alerts.class);
			logger.error(e.getMessage(), e);
		}
	}
}
