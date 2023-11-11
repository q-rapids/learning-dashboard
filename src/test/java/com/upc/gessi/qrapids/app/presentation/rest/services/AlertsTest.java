package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.upc.gessi.qrapids.app.domain.controllers.AlertsController;
import com.upc.gessi.qrapids.app.domain.controllers.MetricsController;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.controllers.StrategicIndicatorsController;
import com.upc.gessi.qrapids.app.domain.exceptions.MetricNotFoundException;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectNotFoundException;
import com.upc.gessi.qrapids.app.domain.models.Alert;
import com.upc.gessi.qrapids.app.domain.models.AlertType;
import com.upc.gessi.qrapids.app.domain.models.Notification;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.testHelpers.DomainObjectsBuilder;
import com.upc.gessi.qrapids.app.testHelpers.HelperFunctions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class AlertsTest {
    private DomainObjectsBuilder domainObjectsBuilder;

    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Mock
    private SimpMessagingTemplate simpleMessagingTemplate;

    @Mock
    private ProjectsController projectsController;

    @Mock
    private MetricsController metricsController;

    @Mock
    private AlertsController alertsController;
    @Mock
    private StrategicIndicatorsController siController;

    @InjectMocks
    private Alerts alertsServiceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(alertsServiceController)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getAllAlerts() throws Exception {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        when(projectsController.findProjectByExternalId(project.getExternalId())).thenReturn(project);

        Alert alert = domainObjectsBuilder.buildAlert(project);
        List<Alert> alertList = new ArrayList<>();
        alert.setPredictionTechnique("PROPHET");
        alert.setPredictionDate(new Date());
        alertList.add(alert);

        when(alertsController.getAllProjectAlertsWithProfile(project.getId(), null)).thenReturn(alertList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/alerts")
                .param("prj", project.getExternalId());

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(alert.getId().intValue())))
                .andExpect(jsonPath("$[0].affectedId", is(alert.getAffectedId())))
                .andExpect(jsonPath("$[0].affectedType", is(alert.getAffectedType())))
                .andExpect(jsonPath("$[0].type", is(alert.getType().toString())))
                .andExpect(jsonPath("$[0].value", is(HelperFunctions.getFloatAsDouble(alert.getValue()))))
                .andExpect(jsonPath("$[0].threshold", is(HelperFunctions.getFloatAsDouble(alert.getThreshold()))))
                .andExpect(jsonPath("$[0].date", is(alert.getDate().getTime())))
                .andExpect(jsonPath("$[0].status", is(alert.getStatus().toString())))
                .andExpect(jsonPath("$[0].predictionDate", is(alert.getPredictionDate().getTime())))
                .andExpect(jsonPath("$[0].predictionTechnique", is(alert.getPredictionTechnique())))
                .andDo(document("alerts/get-all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                        .description("Project external identifier"),
                                parameterWithName("profile")
                                        .description("Profile data base identifier")
                                        .optional()),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Alert identifier"),
                                fieldWithPath("[].affectedId")
                                        .description("Identifier of the affected element causing the alert"),
                                fieldWithPath("[].affectedType")
                                        .description("Type of the affected element causing the alert (metric, factor or indicator)"),
                                fieldWithPath("[].type")
                                        .description("Type of the alert causing the alert"),
                                fieldWithPath("[].value")
                                        .description("Current value of the element causing the alert"),
                                fieldWithPath("[].threshold")
                                        .description("Minimum acceptable value for the element"),
                                fieldWithPath("[].date")
                                        .description("Generation date of the alert"),
                                fieldWithPath("[].status")
                                        .description("Status of the alert (NEW or VIEWED)"),
                                fieldWithPath("[].predictionDate")
                                        .description("Date for the prediction that has raised an alert, if it is a prediction alert"),
                                fieldWithPath("[].predictionTechnique")
                                        .description("Technique used for the prediction that raised an alert, if it is a prediction alert")
                        )
                ));

        // Verify mock interactions
        verify(alertsController, times(1)).getAllProjectAlertsWithProfile(project.getId(),null);
        verify(alertsController, times(alertList.size())).changeAlertStatusToViewed(any());
        verifyNoMoreInteractions(alertsController);
    }

    @Test
    public void getAllAlertsNonExistingProject() throws Exception {
        // Given
        String projectExternalId = "prj";
        when(projectsController.findProjectByExternalId(projectExternalId)).thenThrow(new ProjectNotFoundException(projectExternalId));

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/alerts")
                .param("prj", projectExternalId);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andDo(document("alerts/get-all-wrong-project",
                        preprocessResponse(prettyPrint())
                ));

        // Verify mock interactions
        verifyZeroInteractions(alertsController);
    }

    @Test
    public void countNewAlerts() throws Exception {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        when(projectsController.findProjectByExternalId(project.getExternalId())).thenReturn(project);
        int newAlerts = 23;
        when(alertsController.countNewAlerts(project.getId())).thenReturn(newAlerts);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/alerts/countNew")
                .param("prj", project.getExternalId());
        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("alerts/count-new",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestParameters(
                        parameterWithName("prj")
                                .description("Project external identifier"),
                        parameterWithName("profile")
                                .description("Profile data base identifier")
                                .optional()),
                responseBody()
        ));

        // Verify mock interactions
        verify(alertsController, times(1)).countNewAlertsWithProfile(project.getId(),null);
        verifyNoMoreInteractions(alertsController);
    }

    @Test
    public void countNewAlertsNonExistingProject() throws Exception {
        // Given
        String projectExternalId = "prj";
        when(projectsController.findProjectByExternalId(projectExternalId)).thenThrow(new ProjectNotFoundException(projectExternalId));

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/alerts/countNew")
                .param("prj", projectExternalId);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andDo(document("alerts/count-new-wrong-project",
                        preprocessResponse(prettyPrint())
                ));

        // Verify mock interactions
        verify(projectsController, times(1)).findProjectByExternalId(projectExternalId);
        verifyNoMoreInteractions(projectsController);

        verifyZeroInteractions(alertsController);
    }

    @Test
    public void createAlert() throws Exception {
        // Given
        Project project = domainObjectsBuilder.buildProject();
        when(projectsController.findProjectByExternalId(project.getExternalId())).thenReturn(project);
        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);
        // Perform request
        String affectedId = "Duplication";
        String affectedType = "metric";
        String type = "ALERT_NOT_TREATED";
        float value = 0.4f;
        float threshold = 0.5f;
        Map<String, String> element = new HashMap<>();
        element.put("affectedId", affectedId);
        element.put("affectedType", affectedType);
        element.put("type", type);
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", project.getExternalId());
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andDo(document("alerts/add-alert",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("element.affectedId")
                                        .description("Identifier of the element causing the alert"),
                                fieldWithPath("element.affectedType")
                                        .description("Type of the element causing the alert (metric, factor or indicator)"),
                                fieldWithPath("element.type")
                                        .description("Type of the alert (CATEGORY_DOWNGRADE, CATEGORY_UPGRADE, " +
                                                "TRESPASSED_THRESHOLD, ALERT_NOT_TREATED)"),
                                fieldWithPath("element.value")
                                        .description("Current value of the element causing the alert"),
                                fieldWithPath("element.threshold")
                                        .description("Minimum acceptable value for the element"),
                                fieldWithPath("element.project_id")
                                        .description("Project identifier of the element causing the alert")
                        )
                ));

        // Verify mock interactions
        verify(projectsController, times(1)).findProjectByExternalId(project.getExternalId());
        verifyNoMoreInteractions(projectsController);

        verify(alertsController, times(1)).createAlert(value, threshold, AlertType.valueOf(type), project, affectedId, affectedType, null, null);
        verifyNoMoreInteractions(alertsController);

        verify(simpleMessagingTemplate, times(1)).convertAndSend(eq("/queue/notify"), ArgumentMatchers.any(Notification.class));
        verifyNoMoreInteractions(simpleMessagingTemplate);
    }

    @Test
    public void createAlertWrongType() throws Exception {
        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);

        Map<String, String> element = new HashMap<>();
        String affectedId = "Duplication";
        String affectedType = "metric";
        String type = "wrong_type";
        float value = 0.4f;
        float threshold = 0.5f;
        element.put("affectedId", affectedId);
        element.put("affectedType", affectedType);
        element.put("type", type);
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", "prj");
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andDo(document("alerts/add-alert-wrong-type",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    public void createAlertWrongAffectedType() throws Exception {
        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);

        Map<String, String> element = new HashMap<>();
        String affectedId = "Duplication";
        String affectedType = "wrong_type";
        String type = "ALERT_NOT_TREATED";
        float value = 0.4f;
        float threshold = 0.5f;
        element.put("affectedId", affectedId);
        element.put("affectedType", affectedType);
        element.put("type", type);
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", "prj");
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(is("The affected type provided is not valid. It must be either metric, factor or indicator.")))
                .andDo(document("alerts/add-alert-affected-wrong-type",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    public void createAlertWrongProject() throws Exception {
        //Given
        String projectExternalId = "prj";
        when(projectsController.findProjectByExternalId(projectExternalId)).thenThrow(new ProjectNotFoundException(projectExternalId));
        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);

        Map<String, String> element = new HashMap<>();
        String affectedId = "Duplication";
        String affectedType = "metric";
        String type = "ALERT_NOT_TREATED";
        float value = 0.4f;
        float threshold = 0.5f;
        element.put("affectedId", affectedId);
        element.put("affectedType", affectedType);
        element.put("type", type);
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", projectExternalId);
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);
        //Request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andDo(document("alerts/add-alert-wrong-project",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }
    @Test
    public void createAlertWrongAffectedId() throws Exception {
        //Given
        Project project = domainObjectsBuilder.buildProject();
        when(projectsController.findProjectByExternalId(project.getExternalId())).thenReturn(project);
        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);

        Map<String, String> element = new HashMap<>();
        String affectedId = "wrong_id";
        String affectedType = "metric";
        AlertType type = AlertType.ALERT_NOT_TREATED;
        float value = 0.4f;
        float threshold = 0.5f;
        element.put("affectedId", affectedId);
        element.put("affectedType", affectedType);
        element.put("type", type.toString());
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", project.getExternalId());
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        doThrow(new MetricNotFoundException()).when(alertsController).createAlert(value, threshold, type, project, affectedId, affectedType, null, null);

        //Request
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andDo(document("alerts/add-alert-affected-wrong-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    @Test
    public void addAlertMissingParams() throws Exception {
        Map<String, String> element = new HashMap<>();
        String affectedId = "duplication";
        String affectedType = "metric";
        AlertType type = AlertType.ALERT_NOT_TREATED;
        float value = 0.4f;
        float threshold = 0.5f;
        element.put("affectedId", affectedId);
        //missing affectedType
        element.put("type", type.toString());
        element.put("value", Float.toString(value));
        element.put("threshold", Float.toString(threshold));
        element.put("project_id", "prj");
        Map<String, Map<String, String>> body = new HashMap<>();
        body.put("element", element);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();
        String bodyJson = objectWriter.writeValueAsString(body);

        List<String> forecastTechniques = Arrays.asList("PROPHET");
        when(siController.getForecastTechniques()).thenReturn(forecastTechniques);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andDo(document("alerts/add-alert-missing-param",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

}
