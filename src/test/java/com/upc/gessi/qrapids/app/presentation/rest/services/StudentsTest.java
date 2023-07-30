package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.upc.gessi.qrapids.app.domain.controllers.StudentsController;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.*;

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
        Map<DataSource, DTOStudentIdentity> dTOStudentIdentities = new HashMap<>();

        for(DataSource source: DataSource.values()){
            dTOStudentIdentities.put(source, new DTOStudentIdentity(source,"username"));
        }

        List<DTOMetricEvaluation> dtoMetricEvaluationList = new ArrayList<>();
        dtoMetricEvaluationList.add(domainObjectsBuilder.buildDTOMetric());
        DTOStudentMetrics dtoStudentMetrics = new DTOStudentMetrics(student_name, dTOStudentIdentities, dtoMetricEvaluationList);
        dtoStudentMetricsList.add(dtoStudentMetrics);

        // Given
        when(studentsDomainController.getStudentMetricsFromProject("prjExternalId",null,null,null)).thenReturn(dtoStudentMetricsList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects/metrics/students")
                .param("prj", "prjExternalId");


        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].project", is(nullValue())))
                .andExpect(jsonPath("$[0].id", is(nullValue())))
                .andExpect(jsonPath("$[0].name", is(student_name)))
                .andExpect(jsonPath("$[0].identities.GITHUB.data_source", is(dTOStudentIdentities.get(DataSource.GITHUB).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.GITHUB.username", is(dTOStudentIdentities.get(DataSource.GITHUB).getUsername())))
                .andExpect(jsonPath("$[0].identities.TAIGA.data_source", is(dTOStudentIdentities.get(DataSource.TAIGA).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.TAIGA.username", is(dTOStudentIdentities.get(DataSource.TAIGA).getUsername())))
                .andExpect(jsonPath("$[0].identities.PRT.data_source", is(dTOStudentIdentities.get(DataSource.PRT).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.PRT.username", is(dTOStudentIdentities.get(DataSource.PRT).getUsername())))
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
                                fieldWithPath("[].id")
                                    .description("Student identifier"),
                                fieldWithPath("[].project")
                                        .description("Project if the student"),
                                fieldWithPath("[].name")
                                    .description("Name of the student"),
                                fieldWithPath("[].metrics_size")
                                        .description("Metrics size"),
                                fieldWithPath("[].identities")
                                        .description("Student identities such as Github, Taiga"),
                                fieldWithPath("[].identities.GITHUB")
                                        .description("Student Github identity"),
                                fieldWithPath("[].identities.GITHUB.data_source")
                                        .description("Student Github identity data source"),
                                fieldWithPath("[].identities.GITHUB.username")
                                        .description("Student Github username"),
                                fieldWithPath("[].identities.GITHUB.student")
                                        .description("Github student"),
                                fieldWithPath("[].identities.TAIGA")
                                        .description("Student Taiga identity"),
                                fieldWithPath("[].identities.TAIGA.data_source")
                                        .description("Student Taiga identity data source"),
                                fieldWithPath("[].identities.TAIGA.username")
                                        .description("Student Taiga username"),
                                fieldWithPath("[].identities.TAIGA.student")
                                        .description("Taiga student"),
                                fieldWithPath("[].identities.PRT")
                                        .description("Student PRT identity"),
                                fieldWithPath("[].identities.PRT.data_source")
                                        .description("Student PRT identity data source"),
                                fieldWithPath("[].identities.PRT.username")
                                        .description("Student PRT username"),
                                fieldWithPath("[].identities.PRT.student")
                                        .description("PRT student"),
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
        verify(studentsDomainController, times(1)).getStudentMetricsFromProject("prjExternalId",null,null,null);
        verifyNoMoreInteractions(studentsDomainController);


    }

    @Test
    public void getStudentsAndMetricsHistorical() throws Exception {

        List<DTOStudentMetrics> dtoStudentMetricsList = new ArrayList<>();
        String student_name = "student_test_name";

        Map<DataSource, DTOStudentIdentity> dTOStudentIdentities = new HashMap<>();

        for(DataSource source: DataSource.values()){
            dTOStudentIdentities.put(source, new DTOStudentIdentity(source,"username"));
        }

        String from = "2000-01-01";
        LocalDate localDateFrom = LocalDate.parse(from);
        String to = "2000-05-01";
        LocalDate localDateTo = LocalDate.parse(to);
        List<DTOMetricEvaluation> dtoMetricEvaluationList = new ArrayList<>();
        dtoMetricEvaluationList.add(domainObjectsBuilder.buildDTOMetric());
        DTOStudentMetrics dtoStudentMetrics = new DTOStudentMetrics(
                student_name, dTOStudentIdentities, dtoMetricEvaluationList, 1);
        dtoStudentMetricsList.add(dtoStudentMetrics);

        // Given
        when(studentsDomainController.getStudentMetricsFromProject(
                "prjExternalId", localDateFrom, localDateTo, "profileId"))
                .thenReturn(dtoStudentMetricsList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects/metrics/students/historical")
                .param("prj", "prjExternalId")
                .param("from", String.valueOf(localDateFrom))
                .param("to", String.valueOf(localDateTo))
                .param("profile", "profileId");


        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].project", is(nullValue())))
                .andExpect(jsonPath("$[0].id", is(nullValue())))
                .andExpect(jsonPath("$[0].name", is(student_name)))
                .andExpect(jsonPath("$[0].identities.GITHUB.data_source", is(dTOStudentIdentities.get(DataSource.GITHUB).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.GITHUB.username", is(dTOStudentIdentities.get(DataSource.GITHUB).getUsername())))
                .andExpect(jsonPath("$[0].identities.TAIGA.data_source", is(dTOStudentIdentities.get(DataSource.TAIGA).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.TAIGA.username", is(dTOStudentIdentities.get(DataSource.TAIGA).getUsername())))
                .andExpect(jsonPath("$[0].identities.PRT.data_source", is(dTOStudentIdentities.get(DataSource.PRT).getDataSource().toString())))
                .andExpect(jsonPath("$[0].identities.PRT.username", is(dTOStudentIdentities.get(DataSource.PRT).getUsername())))
                .andExpect(jsonPath("$[0].metrics_size", is(1)))
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
                                fieldWithPath("[].id")
                                        .description("Student identifier"),
                                fieldWithPath("[].project")
                                        .description("Project if the student"),
                                fieldWithPath("[].name")
                                        .description("Name of the student"),
                                fieldWithPath("[].identities")
                                        .description("Student identities such as Github, Taiga"),
                                fieldWithPath("[].identities.GITHUB")
                                        .description("Student Github identity"),
                                fieldWithPath("[].identities.GITHUB.data_source")
                                        .description("Student Github identity data source"),
                                fieldWithPath("[].identities.GITHUB.username")
                                        .description("Student Github username"),
                                fieldWithPath("[].identities.GITHUB.student")
                                        .description("Github student"),
                                fieldWithPath("[].identities.TAIGA")
                                        .description("Student Taiga identity"),
                                fieldWithPath("[].identities.TAIGA.data_source")
                                        .description("Student Taiga identity data source"),
                                fieldWithPath("[].identities.TAIGA.username")
                                        .description("Student Taiga username"),
                                fieldWithPath("[].identities.TAIGA.student")
                                        .description("Taiga student"),
                                fieldWithPath("[].identities.PRT")
                                        .description("Student PRT identity"),
                                fieldWithPath("[].identities.PRT.data_source")
                                        .description("Student PRT identity data source"),
                                fieldWithPath("[].identities.PRT.username")
                                        .description("Student PRT username"),
                                fieldWithPath("[].identities.PRT.student")
                                        .description("PRT student"),
                                fieldWithPath("[].metrics_size")
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
        verify(studentsDomainController, times(1)).getStudentMetricsFromProject("prjExternalId", localDateFrom, localDateTo, "profileId");
        verifyNoMoreInteractions(studentsDomainController);

    }

    @Test
    public void updateMetricStudent() throws Exception {

        Map<DataSource, DTOStudentIdentity> dtoStudentIdentities = new HashMap<>();
        Map<DataSource, String> dtoCreateIdentities = new HashMap<>();

        for(DataSource source: DataSource.values()){
            dtoStudentIdentities.put(source, new DTOStudentIdentity(source,"username"));
            dtoCreateIdentities.put(source, "username");
        }

        String studentName = "student_test_name";

        DTOStudent dtoStudent = new DTOStudent(studentName, dtoStudentIdentities, null);

        DTOCreateStudent dtoCreateStudent = new DTOCreateStudent();
        dtoCreateStudent.setName(studentName);
        dtoCreateStudent.setIdentities(dtoCreateIdentities);
        dtoCreateStudent.setId(1L);
        String projectId="1";

        List<Long> userMetricstemp = new ArrayList<>(Arrays.asList(1L,2L,3L));
        dtoCreateStudent.setMetrics(userMetricstemp);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();


        when(studentsDomainController.updateStudentAndMetrics(dtoCreateStudent.getId(), dtoStudent, dtoCreateStudent.getMetrics(), projectId)).thenReturn(dtoCreateStudent.getId());
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/projects/metrics/students")
                .param("prj", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dtoCreateStudent))
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("students/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                        .description("External id of the student's project")
                        )
                ));

        // Verify mock interactions
        ArgumentCaptor<DTOStudent> argument1 = ArgumentCaptor.forClass(DTOStudent.class);
        ArgumentCaptor<Long> argument2 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List<Long>> argument3 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> argument4 = ArgumentCaptor.forClass(String.class);
        verify(studentsDomainController, times(1)).updateStudentAndMetrics(argument2.capture(), argument1.capture(),argument3.capture(),argument4.capture());
        assertEquals(dtoStudent.getId(), argument1.getValue().getId());
        assertEquals(dtoStudent.getName(), argument1.getValue().getName());

        argument1.getValue().getIdentities().forEach(((dataSource, studentIdentity) -> {
            assertEquals(dtoStudentIdentities.get(dataSource).getUsername(), studentIdentity.getUsername() );
        }));

        assertEquals(dtoCreateStudent.getId(), argument2.getValue());
        assertEquals(dtoCreateStudent.getMetrics().get(0), argument3.getValue().get(0));
        assertEquals(projectId, argument4.getValue());

        verifyNoMoreInteractions(studentsDomainController);

    }

    @Test
    public void deleteMetricStudent() throws Exception {

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .delete("/api/projects/metrics/students/{studentId}", 1L);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("students/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("studentId")
                                        .description("Student identifier")
                        )
                ));

        // Verify mock interactions
        verify(studentsDomainController, times(1)).deleteStudents(1L);
        verifyNoMoreInteractions(studentsDomainController);
    }


}
