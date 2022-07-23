package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.models.Student;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.testHelpers.DomainObjectsBuilder;
import com.upc.gessi.qrapids.app.testHelpers.HelperFunctions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class StudentsTest {

    private DomainObjectsBuilder domainObjectsBuilder;

    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Mock
    private StudentsController studentsDomainController;

    @InjectMocks
    private Metrics metricsController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(metricsController)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getStudentsAndMetrics() throws Exception {

        List<DTOStudentMetrics> dtoStudentMetricsList = new ArrayList<>();
        String student_name = "student_test_name";
        String taiga_username = "taiga_test_username";
        String github_username = "github_test_username";
        List<DTOMetricEvaluation> dtoMetricEvaluationList = new ArrayList<>();
        dtoMetricEvaluationList.add(domainObjectsBuilder.buildDTOMetric());
        DTOStudentMetrics dtoStudentMetrics = new DTOStudentMetrics(student_name, taiga_username, github_username, dtoMetricEvaluationList);
        dtoStudentMetricsList.add(dtoStudentMetrics);

        // Given
        when(studentsDomainController.getStudentWithMetricsFromProject("prjExternalId")).thenReturn(dtoStudentMetricsList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/metrics/student")
                .param("prj", "prjExternalId");


        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].project", is(nullValue())))
                .andExpect(jsonPath("$[0].student_id", is(nullValue())))
                .andExpect(jsonPath("$[0].studentName", is(student_name)))
                .andExpect(jsonPath("$[0].taigaUsername", is(taiga_username)))
                .andExpect(jsonPath("$[0].githubUsername", is(github_username)))
                .andExpect(jsonPath("$[0].metrics[0].id", is(dtoMetricEvaluationList.get(0).getId())))
                .andExpect(jsonPath("$[0].metrics[0].name", is(dtoMetricEvaluationList.get(0).getName())))
                .andExpect(jsonPath("$[0].metrics[0].description", is(dtoMetricEvaluationList.get(0).getDescription())))
                .andExpect(jsonPath("$[0].metrics[0].value", is(HelperFunctions.getFloatAsDouble(dtoMetricEvaluationList.get(0).getValue()))))
                .andExpect(jsonPath("$[0].metrics[0].value_description", is(String.format("%.2f", dtoMetricEvaluationList.get(0).getValue()))))
                .andExpect(jsonPath("$[0].metrics[0].date[0]", is(dtoMetricEvaluationList.get(0).getDate().getYear())))
                .andExpect(jsonPath("$[0].metrics[0].date[1]", is(dtoMetricEvaluationList.get(0).getDate().getMonthValue())))
                .andExpect(jsonPath("$[0].metrics[0].date[2]", is(dtoMetricEvaluationList.get(0).getDate().getDayOfMonth())))
                .andExpect(jsonPath("$[0].metrics[0].datasource", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].rationale", is(dtoMetricEvaluationList.get(0).getRationale())))
                .andExpect(jsonPath("$[0].metrics[0].confidence80", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].confidence95", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].forecastingError", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].qualityFactors", is(dtoMetricEvaluationList.get(0).getQualityFactors())))
                .andDo(document("students/current",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                        .description("Project external identifier")),
                        responseFields(
                                fieldWithPath("[].student_id")
                                    .description("Student identifier"),
                                fieldWithPath("[].project")
                                        .description("Project if the student"),
                                fieldWithPath("[].studentName")
                                    .description("Name of the student"),
                                fieldWithPath("[].taigaUsername")
                                        .description("Taiga username of the student"),
                                fieldWithPath("[].githubUsername")
                                        .description("Github username of the student"),
                                fieldWithPath("[].metrics[].id")
                                        .description("Metric identifier"),
                                fieldWithPath("[].metrics[].name")
                                        .description("Metric name"),
                                fieldWithPath("[].metrics[].description")
                                        .description("Metric description"),
                                fieldWithPath("[].metrics[].value")
                                        .description("Metric value"),
                                fieldWithPath("[].metrics[].value_description")
                                        .description("Metric readable value"),
                                fieldWithPath("[].metrics[].date")
                                        .description("Metric evaluation date"),
                                fieldWithPath("[].metrics[].datasource")
                                        .description("Metric source of data"),
                                fieldWithPath("[].metrics[].rationale")
                                        .description("Metric evaluation rationale"),
                                fieldWithPath("[].metrics[].confidence80")
                                        .description("Metric forecasting 80% confidence interval"),
                                fieldWithPath("[].metrics[].confidence95")
                                        .description("Metric forecasting 95% confidence interval"),
                                fieldWithPath("[].metrics[].forecastingError")
                                        .description("Description of forecasting errors"),
                                fieldWithPath("[].metrics[].qualityFactors")
                                        .description("List of the quality factors that use this metric")
                        )
                ));

        // Verify mock interactions
        verify(studentsDomainController, times(1)).getStudentWithMetricsFromProject("prjExternalId");
        verifyNoMoreInteractions(studentsDomainController);


    }

    @Test
    public void getStudentsAndMetricsHistorical() throws Exception {

        List<DTOStudentMetricsHistorical> dtoStudentMetricsList = new ArrayList<>();
        String student_name = "student_test_name";
        String taiga_username = "taiga_test_username";
        String github_username = "github_test_username";
        String from = "2000-01-01";
        LocalDate localDateFrom = LocalDate.parse(from);
        String to = "2000-05-01";
        LocalDate localDateTo = LocalDate.parse(to);
        List<DTOMetricEvaluation> dtoMetricEvaluationList = new ArrayList<>();
        dtoMetricEvaluationList.add(domainObjectsBuilder.buildDTOMetric());
        DTOStudentMetricsHistorical dtoStudentMetrics = new DTOStudentMetricsHistorical(student_name, taiga_username, github_username, dtoMetricEvaluationList, 1);
        dtoStudentMetricsList.add(dtoStudentMetrics);

        // Given
        when(studentsDomainController.getStudentWithHistoricalMetricsFromProject("prjExternalId", localDateFrom, localDateTo, "profileId")).thenReturn(dtoStudentMetricsList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/metrics/student/historical")
                .param("prj", "prjExternalId")
                .param("from", String.valueOf(localDateFrom))
                .param("to", String.valueOf(localDateTo))
                .param("profile", "profileId");


        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].project", is(nullValue())))
                .andExpect(jsonPath("$[0].student_id", is(nullValue())))
                .andExpect(jsonPath("$[0].studentName", is(student_name)))
                .andExpect(jsonPath("$[0].taigaUsername", is(taiga_username)))
                .andExpect(jsonPath("$[0].githubUsername", is(github_username)))
                .andExpect(jsonPath("$[0].numberMetrics", is(1)))
                .andExpect(jsonPath("$[0].metrics[0].id", is(dtoMetricEvaluationList.get(0).getId())))
                .andExpect(jsonPath("$[0].metrics[0].name", is(dtoMetricEvaluationList.get(0).getName())))
                .andExpect(jsonPath("$[0].metrics[0].description", is(dtoMetricEvaluationList.get(0).getDescription())))
                .andExpect(jsonPath("$[0].metrics[0].value", is(HelperFunctions.getFloatAsDouble(dtoMetricEvaluationList.get(0).getValue()))))
                .andExpect(jsonPath("$[0].metrics[0].value_description", is(String.format("%.2f", dtoMetricEvaluationList.get(0).getValue()))))
                .andExpect(jsonPath("$[0].metrics[0].date[0]", is(dtoMetricEvaluationList.get(0).getDate().getYear())))
                .andExpect(jsonPath("$[0].metrics[0].date[1]", is(dtoMetricEvaluationList.get(0).getDate().getMonthValue())))
                .andExpect(jsonPath("$[0].metrics[0].date[2]", is(dtoMetricEvaluationList.get(0).getDate().getDayOfMonth())))
                .andExpect(jsonPath("$[0].metrics[0].datasource", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].rationale", is(dtoMetricEvaluationList.get(0).getRationale())))
                .andExpect(jsonPath("$[0].metrics[0].confidence80", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].confidence95", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].forecastingError", is(nullValue())))
                .andExpect(jsonPath("$[0].metrics[0].qualityFactors", is(dtoMetricEvaluationList.get(0).getQualityFactors())))
                .andDo(document("students/historical",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                    .description("Project external identifier"),
                                parameterWithName("profile")
                                    .description("Profile identifier"),
                                parameterWithName("from")
                                    .description("Initial date"),
                                parameterWithName("to")
                                    .description("Final Date")),
                        responseFields(
                                fieldWithPath("[].student_id")
                                        .description("Student identifier"),
                                fieldWithPath("[].project")
                                        .description("Project if the student"),
                                fieldWithPath("[].studentName")
                                        .description("Name of the student"),
                                fieldWithPath("[].taigaUsername")
                                        .description("Taiga username of the student"),
                                fieldWithPath("[].githubUsername")
                                        .description("Github username of the student"),
                                fieldWithPath("[].numberMetrics")
                                        .description("Number of metrics"),
                                fieldWithPath("[].metrics[].id")
                                        .description("Metric identifier"),
                                fieldWithPath("[].metrics[].name")
                                        .description("Metric name"),
                                fieldWithPath("[].metrics[].description")
                                        .description("Metric description"),
                                fieldWithPath("[].metrics[].value")
                                        .description("Metric value"),
                                fieldWithPath("[].metrics[].value_description")
                                        .description("Metric readable value"),
                                fieldWithPath("[].metrics[].date")
                                        .description("Metric evaluation date"),
                                fieldWithPath("[].metrics[].datasource")
                                        .description("Metric source of data"),
                                fieldWithPath("[].metrics[].rationale")
                                        .description("Metric evaluation rationale"),
                                fieldWithPath("[].metrics[].confidence80")
                                        .description("Metric forecasting 80% confidence interval"),
                                fieldWithPath("[].metrics[].confidence95")
                                        .description("Metric forecasting 95% confidence interval"),
                                fieldWithPath("[].metrics[].forecastingError")
                                        .description("Description of forecasting errors"),
                                fieldWithPath("[].metrics[].qualityFactors")
                                        .description("List of the quality factors that use this metric")
                        )
                ));

        // Verify mock interactions
        verify(studentsDomainController, times(1)).getStudentWithHistoricalMetricsFromProject("prjExternalId", localDateFrom, localDateTo, "profileId");
        verifyNoMoreInteractions(studentsDomainController);

    }

    @Test
    public void updateMetricStudent() throws Exception {

        DTOStudent dtoStudent = new DTOStudent("student_test_name", "taiga_test_name", "github_test_name", null);
        String[] userMetricstemp = { "s1", "s2", "s3" };
        String studentsList = "student_test_name,taiga_test_name,github_test_name";
        String studentId="1";
        String projectId="1";
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/metrics/student")
                .param("userTemp", userMetricstemp)
                .param("studentId", studentId)
                .param("projectId", projectId)
                .param("studentsList", studentsList)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("students/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("userTemp")
                                        .description("List of metrics ids that belong to the student"),
                                parameterWithName("studentId")
                                        .description("Id of the student"),
                                parameterWithName("studentsList")
                                        .description("String with the name and username of the students, separated by a comma"),
                                parameterWithName("projectId")
                                        .description("Id of the student's project")
                        )
                ));

        // Verify mock interactions
        ArgumentCaptor<DTOStudent> argument1 = ArgumentCaptor.forClass(DTOStudent.class);
        ArgumentCaptor<String> argument2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String[]> argument3 = ArgumentCaptor.forClass(String[].class);
        ArgumentCaptor<String> argument4 = ArgumentCaptor.forClass(String.class);
        verify(studentsDomainController, times(1)).updateStudents(argument2.capture(), argument1.capture(),argument3.capture(),argument4.capture());
        assertEquals(dtoStudent.getStudent_id(), argument1.getValue().getStudent_id());
        assertEquals(dtoStudent.getStudentName(), argument1.getValue().getStudentName());
        assertEquals(dtoStudent.getTaigaUsername(), argument1.getValue().getTaigaUsername());
        assertEquals(dtoStudent.getGithubUsername(), argument1.getValue().getGithubUsername());
        assertEquals(studentId, argument2.getValue());
        assertEquals(userMetricstemp[0], argument3.getValue()[0]);
        assertEquals(projectId, argument4.getValue());
        verifyNoMoreInteractions(studentsDomainController);

    }

    @Test
    public void deleteMetricStudent() throws Exception {

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .delete("/api/metrics/student/{id}", 1L);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("students/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Student identifier")
                        )
                ));

        // Verify mock interactions
        verify(studentsDomainController, times(1)).deleteStudents(1L);
        verifyNoMoreInteractions(studentsDomainController);
    }


}
