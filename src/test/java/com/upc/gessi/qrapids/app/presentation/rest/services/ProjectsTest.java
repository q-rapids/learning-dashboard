package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.upc.gessi.qrapids.QrapidsApplication;
import com.upc.gessi.qrapids.app.domain.controllers.ProjectsController;
import com.upc.gessi.qrapids.app.domain.exceptions.ProjectAlreadyAnonymizedException;
import com.upc.gessi.qrapids.app.domain.models.DataSource;
import com.upc.gessi.qrapids.app.domain.models.Project;
import com.upc.gessi.qrapids.app.domain.utils.AnonymizationModes;
import com.upc.gessi.qrapids.app.presentation.rest.dto.*;
import com.upc.gessi.qrapids.app.domain.exceptions.CategoriesException;
import com.upc.gessi.qrapids.app.presentation.rest.services.exceptions.ResourceNotFoundException;
import com.upc.gessi.qrapids.app.presentation.rest.services.helpers.Messages;
import com.upc.gessi.qrapids.app.testHelpers.DomainObjectsBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProjectsTest {

    private DomainObjectsBuilder domainObjectsBuilder;

    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Mock
    private ProjectsController projectsDomainController;

    @InjectMocks
    private Projects projectsController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(projectsController)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getProjectsAndUpdateDB() throws Exception {
        List<String> projectsList = new ArrayList<>();
        String project1 = "project1";
        projectsList.add(project1);
        String project2 = "project2";
        projectsList.add(project2);
        String project3 = "project3";
        projectsList.add(project3);
        when(projectsController.importProjects()).thenReturn(projectsList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects/import");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", is(project1)))
                .andExpect(jsonPath("$[1]", is(project2)))
                .andExpect(jsonPath("$[2]", is(project3)))
                .andDo(document("projects/import",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[]")
                                        .description("List with the external identifiers of all the assessed projects"))
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).importProjectsAndUpdateDatabase();
    }

    @Test
    public void getProjectsCategoriesConflict() throws Exception {
        when(projectsDomainController.importProjectsAndUpdateDatabase()).thenThrow(new CategoriesException(Messages.CATEGORIES_DO_NOT_MATCH));

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects/import");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isConflict())
                .andExpect(status().reason(is("The categories do not match")))
                .andDo(document("projects/import-conflict",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).importProjectsAndUpdateDatabase();
    }

    @Test
    public void getProjectsWithReadError() throws Exception {
        when(projectsDomainController.importProjectsAndUpdateDatabase()).thenThrow(new IOException());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects/import");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isInternalServerError())
                .andExpect(status().reason(is("Error on MongoDB connection")))
                .andDo(document("projects/import-read-error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).importProjectsAndUpdateDatabase();
    }

    @Test
    public void getProjects() throws Exception {
        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = false;
        // PROJECT DTO
        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));

        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false,dtoProjectIdentities, anonymized);

        List<DTOProject> dtoProjectList = new ArrayList<>();
        dtoProjectList.add(dtoProject);

        when(projectsDomainController.getProjects(null)).thenReturn(dtoProjectList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(projectId.intValue())))
                .andExpect(jsonPath("$[0].externalId", is(projectExternalId)))
                .andExpect(jsonPath("$[0].name", is(projectName)))
                .andExpect(jsonPath("$[0].description", is(projectDescription)))
                .andExpect(jsonPath("$[0].logo", is(nullValue())))
                .andExpect(jsonPath("$[0].active", is(active)))
                .andExpect(jsonPath("$[0].backlogId", is(projectBacklogId)))
                .andExpect(jsonPath("$[0].identities.GITHUB.dataSource", is(DataSource.GITHUB.toString())))
                .andExpect(jsonPath("$[0].identities.GITHUB.url", is(identityURL)))
                .andExpect(jsonPath("$[0].isGlobal",is(false)))
                .andExpect(jsonPath("$[0].students", is(nullValue())))
                .andExpect(jsonPath("$[0].anonymized", is(anonymized)))
                .andDo(document("projects/all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("profile_id")
                                        .description("Profile data base identifier (Optional)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Project identifier"),
                                fieldWithPath("[].externalId")
                                        .description("Project external identifier"),
                                fieldWithPath("[].name")
                                        .description("Project name"),
                                fieldWithPath("[].description")
                                        .description("Project description"),
                                fieldWithPath("[].logo")
                                        .description("Project logo file"),
                                fieldWithPath("[].active")
                                        .description("Is an active project?"),
                                fieldWithPath("[].backlogId")
                                        .description("Project identifier in the backlog"),
                                fieldWithPath("[].anonymized")
                                        .description("If project students are anonymized"),
                                fieldWithPath("[].identities")
                                        .description("Project identities"),
                                fieldWithPath("[].identities.GITHUB")
                                        .description("Example of identity, URLs separated by a ';'"),
                                fieldWithPath("[].identities.GITHUB.dataSource")
                                        .description("Identity data source. Example: Github, Taiga, PRT"),
                                fieldWithPath("[].identities.GITHUB.url")
                                        .description("Identity URL"),
                                fieldWithPath("[].identities.GITHUB.project")
                                        .description("Identity project"),
                                fieldWithPath("[].isGlobal")
                                        .description("Is a global project?"),
                                fieldWithPath("[].students")
                                        .description("Students of the project"),
                                fieldWithPath("[].anonymized")
                                        .description("If project is anonymized"))
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).getProjects(null);
        verifyNoMoreInteractions(projectsDomainController);
    }

    @Test
    public void getProjectsByProfile() throws Exception {
        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = false;
        // PROJECT DTO
        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));

        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false,dtoProjectIdentities, anonymized);

        List<DTOProject> dtoProjectList = new ArrayList<>();
        dtoProjectList.add(dtoProject);
        Long profileID = 1L;

        when(projectsDomainController.getProjects(profileID)).thenReturn(dtoProjectList);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/projects")
                .param("profile_id", String.valueOf(profileID));

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(projectId.intValue())))
                .andExpect(jsonPath("$[0].externalId", is(projectExternalId)))
                .andExpect(jsonPath("$[0].name", is(projectName)))
                .andExpect(jsonPath("$[0].description", is(projectDescription)))
                .andExpect(jsonPath("$[0].logo", is(nullValue())))
                .andExpect(jsonPath("$[0].active", is(active)))
                .andExpect(jsonPath("$[0].backlogId", is(projectBacklogId)))
                .andExpect(jsonPath("$[0].identities.GITHUB.dataSource", is(DataSource.GITHUB.toString())))
                .andExpect(jsonPath("$[0].identities.GITHUB.url", is(identityURL)))
                .andExpect(jsonPath("$[0].isGlobal",is(false)))
                .andExpect(jsonPath("$[0].students", is(nullValue())))
                .andExpect(jsonPath("$[0].anonymized", is(anonymized)))
                .andDo(document("profile/projects/all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Project identifier"),
                                fieldWithPath("[].externalId")
                                        .description("Project external identifier"),
                                fieldWithPath("[].name")
                                        .description("Project name"),
                                fieldWithPath("[].description")
                                        .description("Project description"),
                                fieldWithPath("[].logo")
                                        .description("Project logo file"),
                                fieldWithPath("[].active")
                                        .description("Is an active project?"),
                                fieldWithPath("[].backlogId")
                                        .description("Project identifier in the backlog"),
                                fieldWithPath("[].anonymized")
                                        .description("If project students are anonymized"),
                                fieldWithPath("[].identities")
                                        .description("Project identities"),
                                fieldWithPath("[].identities.GITHUB")
                                        .description("Example of identity, URLs separated by a ';'"),
                                fieldWithPath("[].identities.GITHUB.dataSource")
                                        .description("Identity data source. Example: Github, Taiga, PRT"),
                                fieldWithPath("[].identities.GITHUB.url")
                                        .description("Identity URL"),
                                fieldWithPath("[].identities.GITHUB.project")
                                        .description("Identity project"),
                                fieldWithPath("[].isGlobal")
                                        .description("Is a global project?"),
                                fieldWithPath("[].students")
                                        .description("Students of the project"),
                                fieldWithPath("[].anonymized")
                                        .description("If project is anonymized"))
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).getProjects(profileID);
        verifyNoMoreInteractions(projectsDomainController);
    }

    @Test
    public void updateProject() throws Exception {
        Long projectId = 1L;


        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        String projectBacklogId = "999";
        boolean anonymized = false;

        Boolean isGlobal = false;
        // getResource() : The name of a resource is a '/'-separated path name that identifies the resource.
        URL projectImageUrl = QrapidsApplication.class.getClassLoader().getResource("static" + "/" + "icons" + "/" + "projectDefault.jpg");
        File file = new File(projectImageUrl.getPath());
        MockMultipartFile logoMultipartFile = new MockMultipartFile("file", "logo.jpg", "image/jpeg", Files.readAllBytes(file.toPath()));

        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        Map<DataSource, String> dtoProjectIdentitiesBody = new HashMap<>();

        for (DataSource dataSource : DataSource.values()) {
            String dataSourceURL = dataSource.toString() + ".test";
            dtoProjectIdentities.put(dataSource, new DTOProjectIdentity(dataSource, dataSourceURL));
            dtoProjectIdentitiesBody.put(dataSource, dataSourceURL);
        }

        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, logoMultipartFile.getBytes(), true, projectBacklogId, false, dtoProjectIdentities, anonymized);

        DTOUpdateProject dtoUpdateProject = new DTOUpdateProject(projectExternalId, projectName, projectDescription, projectBacklogId, dtoProjectIdentitiesBody, isGlobal);
        when(projectsDomainController.checkProjectByName(projectId, projectName)).thenReturn(true);
        when(projectsDomainController.getProjectDTOById(projectId)).thenReturn(dtoProject);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson = ow.writeValueAsString(dtoUpdateProject);

        MockMultipartFile jsonFile = new MockMultipartFile("data", "", "application/json", requestJson.getBytes());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/projects/{id}", projectId)
                .file(logoMultipartFile)
                .file(jsonFile)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("projects/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParts(
                                partWithName("file")
                                        .description("Project logo file").optional(),
                                partWithName("data")
                                        .description("JSON Project body:" +
                                                "\n {\"external_id\": \"<project-external-id>\",\n" +
                                                "    \"name\": \"<project-name>\",\n" +
                                                "    \"description\": \"<description>\",\n" +
                                                "    \"backlog_id\": \"<backlog>\",\n" +
                                                "    \"global\": <true/false>,\n" +
                                                "    \"identities\" :{\n" +
                                                "        \"<IDENTITY>\": \"<URL>\"\n}" +
                                                "    }")
                        )
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).checkProjectByName(projectId, projectName);

        ArgumentCaptor<DTOProject> argument = ArgumentCaptor.forClass(DTOProject.class);
        verify(projectsDomainController, times(1)).updateProject(argument.capture());
        assertEquals(dtoProject.getId(), argument.getValue().getId());
        assertEquals(dtoProject.getExternalId(), argument.getValue().getExternalId());
        assertEquals(dtoProject.getName(), argument.getValue().getName());
        assertEquals(dtoProject.getDescription(), argument.getValue().getDescription());
        assertEquals(dtoProject.getActive(), argument.getValue().getActive());
        assertEquals(dtoProject.getExternalId(), argument.getValue().getExternalId());

        dtoProject.getIdentities().values().forEach(identity -> {
            DTOProjectIdentity argumentIdentity = argument.getValue().getIdentities().get(identity.getDataSource());
            assertEquals(identity.getDataSource(), argumentIdentity.getDataSource());
            assertEquals(identity.getUrl(), argumentIdentity.getUrl());
            assertEquals(identity.getProject(), argumentIdentity.getProject());
        });


    }

    @Test
    public void updateProjectNameAlreadyExists() throws Exception {
        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        String projectBacklogId = "999";
        Boolean isGlobal = false;
        boolean anonymized = false;

        URL projectImageUrl = QrapidsApplication.class.getClassLoader().getResource("static" + "/" + "icons" + "/" + "projectDefault.jpg");
        File file = new File(projectImageUrl.getPath());
        MockMultipartFile logoMultipartFile = new MockMultipartFile("file", "logo.jpg", "image/jpeg", Files.readAllBytes(file.toPath()));

        Map<DataSource, String> dtoProjectIdentitiesBody = new HashMap<>();
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();

        for (DataSource dataSource : DataSource.values()) {
            String dataSourceURL = dataSource.toString() + ".test";
            dtoProjectIdentitiesBody.put(dataSource, dataSourceURL);
            dtoProjectIdentities.put(dataSource, new DTOProjectIdentity(dataSource, dataSourceURL));

        }


        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, logoMultipartFile.getBytes(), true, projectBacklogId, false, dtoProjectIdentities, anonymized);


        DTOUpdateProject dtoUpdateProject = new DTOUpdateProject(projectExternalId, projectName, projectDescription, projectBacklogId, dtoProjectIdentitiesBody, isGlobal);
        when(projectsDomainController.checkProjectByName(projectId, projectName)).thenReturn(false);
        when(projectsDomainController.getProjectDTOById(projectId)).thenReturn(dtoProject);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson = ow.writeValueAsString(dtoUpdateProject);

        MockMultipartFile jsonFile = new MockMultipartFile("data", "", "application/json", requestJson.getBytes());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/projects/{id}", projectId)
                .file(logoMultipartFile)
                .file(jsonFile)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isConflict())
                .andDo(document("projects/update-error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).checkProjectByName(projectId, projectName);

    }

    @Test
    public void updateProjectBadRequest() throws Exception {
        Long projectId = 1L;

        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        String projectBacklogId = "999";
        Boolean isGlobal = false;

        URL projectImageUrl = QrapidsApplication.class.getClassLoader().getResource("static" + "/" + "icons" + "/" + "projectDefault.jpg");
        File file = new File(projectImageUrl.getPath());
        MockMultipartFile logoMultipartFile = new MockMultipartFile("file", "logo.jpg", "image/jpeg", Files.readAllBytes(file.toPath()));

        Map<DataSource, String> dtoProjectIdentitiesBody = new HashMap<>();

        for (DataSource dataSource : DataSource.values()) {
            String dataSourceURL = dataSource.toString() + ".test";
            dtoProjectIdentitiesBody.put(dataSource, dataSourceURL);
        }

        DTOUpdateProject dtoUpdateProject = new DTOUpdateProject(projectExternalId, projectName, projectDescription, projectBacklogId, dtoProjectIdentitiesBody, isGlobal);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        String requestJson = ow.writeValueAsString(dtoUpdateProject);

        MockMultipartFile jsonFile = new MockMultipartFile("no-data", "", "application/json", requestJson.getBytes());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/projects/{id}", projectId)
                .file(logoMultipartFile)
                .file(jsonFile)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

        when(projectsDomainController.checkProjectByName(projectId, projectName)).thenReturn(false);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andDo(document("projects/update-error-bad-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));

        verifyNoMoreInteractions(projectsDomainController);
    }

    @Test
    public void getProjectById() throws Exception {
        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = false;

        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));
        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false, dtoProjectIdentities, anonymized);

        when(projectsDomainController.getProjectDTOById(projectId)).thenReturn(dtoProject);

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .get("/api/projects/{id}", projectId);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(projectId.intValue())))
                .andExpect(jsonPath("$.externalId", is(projectExternalId)))
                .andExpect(jsonPath("$.name", is(projectName)))
                .andExpect(jsonPath("$.description", is(projectDescription)))
                .andExpect(jsonPath("$.logo", is(nullValue())))
                .andExpect(jsonPath("$.active", is(active)))
                .andExpect(jsonPath("$.backlogId", is(projectBacklogId)))
                .andExpect(jsonPath("$.identities.GITHUB.dataSource", is(DataSource.GITHUB.toString())))
                .andExpect(jsonPath("$.identities.GITHUB.url", is(identityURL)))
                .andExpect(jsonPath("$.isGlobal",is(false)))
                .andExpect(jsonPath("$.students", is(nullValue())))
                .andExpect(jsonPath("$.anonymized", is(anonymized)))
                .andDo(document("projects/single",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Project identifier")
                        ),
                        responseFields(
                                fieldWithPath("id")
                                        .description("Project identifier"),
                                fieldWithPath("externalId")
                                        .description("Project external identifier"),
                                fieldWithPath("name")
                                        .description("Project name"),
                                fieldWithPath("description")
                                        .description("Project description"),
                                fieldWithPath("logo")
                                        .description("Project logo file"),
                                fieldWithPath("active")
                                        .description("Is an active project?"),
                                fieldWithPath("backlogId")
                                        .description("Project identifier in the backlog"),
                                fieldWithPath("anonymized")
                                        .description("If project students are anonymized"),
                                fieldWithPath("identities")
                                        .description("Project identities"),
                                fieldWithPath("identities.GITHUB")
                                        .description("Example of identity, URLs separated by a ';'"),
                                fieldWithPath("identities.GITHUB.dataSource")
                                        .description("Identity data source. Example: Github, Taiga, PRT"),
                                fieldWithPath("identities.GITHUB.url")
                                        .description("Identity URL"),
                                fieldWithPath("identities.GITHUB.project")
                                        .description("Identity project"),
                                fieldWithPath("isGlobal")
                                        .description("Is a global project?"),
                                fieldWithPath("students")
                                        .description("Students of the project"),
                                fieldWithPath("anonymized")
                                        .description("If project is anonymized"))
                ));

        // Verify mock interactions
        verify(projectsDomainController, times(1)).getProjectDTOById(projectId);
        verifyNoMoreInteractions(projectsDomainController);
    }

    @Test
    public void getNextMilestones () throws Exception {
        // Given
        String projectExternalId = "test";
        List<DTOMilestone> milestoneList = domainObjectsBuilder.buildDTOMilestoneList();
        LocalDate now = LocalDate.now();

        when(projectsDomainController.getMilestonesForProject(eq(projectExternalId), eq(now))).thenReturn(milestoneList);

        //Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/milestones")
                .param("prj", projectExternalId)
                .param("date", now.toString());

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date", is(milestoneList.get(0).getDate())))
                .andExpect(jsonPath("$[0].name", is(milestoneList.get(0).getName())))
                .andExpect(jsonPath("$[0].description", is(milestoneList.get(0).getDescription())))
                .andExpect(jsonPath("$[0].type", is(milestoneList.get(0).getType())))
                .andDo(document("milestones/get-from-date",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                        .description("Project external identifier"),
                                parameterWithName("date")
                                        .optional()
                                        .description("Minimum milestone date (yyyy-mm-dd)")
                        ),
                        responseFields(
                                fieldWithPath("[].date")
                                        .description("Milestone date"),
                                fieldWithPath("[].name")
                                        .description("Milestone name"),
                                fieldWithPath("[].description")
                                        .description("Milestone description"),
                                fieldWithPath("[].type")
                                        .description("Milestone type"))
                ));
    }

    @Test
    public void getAllMilestones () throws Exception {
        // Given
        String projectExternalId = "test";
        List<DTOMilestone> milestoneList = domainObjectsBuilder.buildDTOMilestoneList();

        when(projectsDomainController.getMilestonesForProject(projectExternalId, null)).thenReturn(milestoneList);

        //Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/milestones")
                .param("prj", projectExternalId);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date", is(milestoneList.get(0).getDate())))
                .andExpect(jsonPath("$[0].name", is(milestoneList.get(0).getName())))
                .andExpect(jsonPath("$[0].description", is(milestoneList.get(0).getDescription())))
                .andExpect(jsonPath("$[0].type", is(milestoneList.get(0).getType())));
    }

    @Test
    public void getAllPhases () throws Exception {
        // Given
        String projectExternalId = "test";
        List<DTOPhase> phaseList = domainObjectsBuilder.buildDTOPhaseList();

        when(projectsDomainController.getPhasesForProject(projectExternalId, null)).thenReturn(phaseList);

        //Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/phases")
                .param("prj", projectExternalId);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dateFrom", is(phaseList.get(0).getDateFrom())))
                .andExpect(jsonPath("$[0].name", is(phaseList.get(0).getName())))
                .andExpect(jsonPath("$[0].description", is(phaseList.get(0).getDescription())))
                .andExpect(jsonPath("$[0].dateTo", is(phaseList.get(0).getDateTo())))
                .andDo(document("phases/get-from-date",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("prj")
                                        .description("Project external identifier"),
                                parameterWithName("date")
                                        .optional()
                                        .description("Minimum phase date (yyyy-mm-dd)")
                        ),
                        responseFields(
                                fieldWithPath("[].dateFrom")
                                        .description("Phase from date"),
                                fieldWithPath("[].name")
                                        .description("Phase name"),
                                fieldWithPath("[].description")
                                        .description("Phase description"),
                                fieldWithPath("[].dateTo")
                                        .description("Phase to date"))
                ));
    }


    @Test
    public void anonymizeProject() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = false;


        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));
        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false, dtoProjectIdentities, anonymized);
        Project project = new Project(projectExternalId, projectName, projectDescription, null, active, false);
        project.setId(projectId);
        project.setAnonymized(anonymized);
        when(projectsDomainController.getProjectDTOById(projectId)).thenReturn(dtoProject);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        Map<String, String> dto = new HashMap<>();
        dto.put("anonymization_mode", mode.toString());

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .post("/api/projects/{id}/anonymize", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));

        dtoProject.setAnonymized(true);
        when(projectsDomainController.anonymizeProject(any(), any())).thenReturn(dtoProject);

        when(projectsDomainController.getProjectById(projectId)).thenReturn(project);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("projects/single-anonymize",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Project identifier")
                        ),
                        requestFields(
                                fieldWithPath("anonymization_mode")
                                        .description("Anonymization mode, if not defined, default is Capitals")
                        ),
                        responseFields(
                                fieldWithPath("id")
                                        .description("Project identifier"),
                                fieldWithPath("externalId")
                                        .description("Project external identifier"),
                                fieldWithPath("name")
                                        .description("Project name"),
                                fieldWithPath("description")
                                        .description("Project description"),
                                fieldWithPath("logo")
                                        .description("Project logo file"),
                                fieldWithPath("active")
                                        .description("Is an active project?"),
                                fieldWithPath("backlogId")
                                        .description("Project identifier in the backlog"),
                                fieldWithPath("anonymized")
                                        .description("If project students are anonymized"),
                                fieldWithPath("identities")
                                        .description("Project identities"),
                                fieldWithPath("identities.GITHUB")
                                        .description("Example of identity, URLs separated by a ';'"),
                                fieldWithPath("identities.GITHUB.dataSource")
                                        .description("Identity data source. Example: Github, Taiga, PRT"),
                                fieldWithPath("identities.GITHUB.url")
                                        .description("Identity URL"),
                                fieldWithPath("identities.GITHUB.project")
                                        .description("Identity project"),
                                fieldWithPath("isGlobal")
                                        .description("Is a global project?"),
                                fieldWithPath("students")
                                        .description("Students of the project"))
                ));
    }

    @Test
    public void anonymizeProjectNotFound() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        Map<String, String> dto = new HashMap<>();
        dto.put("anonymization_mode", mode.toString());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/{id}/anonymize", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));

        when(projectsDomainController.getProjectById(projectId)).thenThrow(ResourceNotFoundException.class);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andDo(document("projects/single-anonymize/error-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));

    }
    @Test
    public void anonymizeProjectBadRequest() throws Exception {

        Long projectId = 1L;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        Map<String, String> dto = new HashMap<>();
        dto.put("anonymization_mode", "YEPA");

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/{id}/anonymize", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andDo(document("projects/single-anonymize/error-bad-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    public void anonymizeProjectAlreadyAnonimizate() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = true;


        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));
        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false, dtoProjectIdentities, anonymized);
        Project project = new Project(projectExternalId, projectName, projectDescription, null, active, false);
        project.setId(projectId);
        project.setAnonymized(anonymized);

        when(projectsDomainController.getProjectById(projectId)).thenReturn(project);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        Map<String, String> dto = new HashMap<>();
        dto.put("anonymization_mode", mode.toString());

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/{id}/anonymize", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));



        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isConflict())
                .andDo(document("projects/single-anonymize/error-conflict",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }


    @Test
    public void anonymizeProjects() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;
        String projectExternalId = "test";
        String projectName = "Test";
        String projectDescription = "Test project";
        boolean active = true;
        String projectBacklogId = "999";
        boolean anonymized = false;


        String identityURL = "githubURL";
        Map<DataSource, DTOProjectIdentity> dtoProjectIdentities = new HashMap<>();
        dtoProjectIdentities.put(DataSource.GITHUB, new DTOProjectIdentity(DataSource.GITHUB, identityURL));
        DTOProject dtoProject = new DTOProject(projectId, projectExternalId, projectName, projectDescription, null, active, projectBacklogId, false, dtoProjectIdentities, anonymized);

        List<DTOProject> dtoProjects = new ArrayList<>();
        dtoProjects.add(dtoProject);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        List<Long> projectIds = new ArrayList<>();
        projectIds.add(projectId);

        Map<String, Object> dto = new HashMap<>();
        dto.put("anonymization_mode", mode.toString());
        dto.put("project_ids", projectIds);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/anonymize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));


        dtoProjects.get(0).setAnonymized(true);
        when(projectsDomainController.anonymizeProjects(any(), any())).thenReturn(dtoProjects);

        this.mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(projectId.intValue())))
                .andExpect(jsonPath("$[0].externalId", is(projectExternalId)))
                .andExpect(jsonPath("$[0].name", is(projectName)))
                .andExpect(jsonPath("$[0].description", is(projectDescription)))
                .andExpect(jsonPath("$[0].logo", is(nullValue())))
                .andExpect(jsonPath("$[0].active", is(active)))
                .andExpect(jsonPath("$[0].backlogId", is(projectBacklogId)))
                .andExpect(jsonPath("$[0].identities.GITHUB.dataSource", is(DataSource.GITHUB.toString())))
                .andExpect(jsonPath("$[0].identities.GITHUB.url", is(identityURL)))
                .andExpect(jsonPath("$[0].isGlobal",is(false)))
                .andExpect(jsonPath("$[0].students", is(nullValue())))
                .andExpect(jsonPath("$[0].anonymized", is(true)))
                .andDo(document("projects/anonymize",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("project_ids")
                                        .description("List of project ids to anonymize"),
                                fieldWithPath("anonymization_mode")
                                        .description("Anonymization mode, if not defined, default is Capitals")
                        ),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Project identifier"),
                                fieldWithPath("[].externalId")
                                        .description("Project external identifier"),
                                fieldWithPath("[].name")
                                        .description("Project name"),
                                fieldWithPath("[].description")
                                        .description("Project description"),
                                fieldWithPath("[].logo")
                                        .description("Project logo file"),
                                fieldWithPath("[].active")
                                        .description("Is an active project?"),
                                fieldWithPath("[].backlogId")
                                        .description("Project identifier in the backlog"),
                                fieldWithPath("[].anonymized")
                                        .description("If project students are anonymized"),
                                fieldWithPath("[].identities")
                                        .description("Project identities"),
                                fieldWithPath("[].identities.GITHUB")
                                        .description("Example of identity, URLs separated by a ';'"),
                                fieldWithPath("[].identities.GITHUB.dataSource")
                                        .description("Identity data source. Example: Github, Taiga, PRT"),
                                fieldWithPath("[].identities.GITHUB.url")
                                        .description("Identity URL"),
                                fieldWithPath("[].identities.GITHUB.project")
                                        .description("Identity project"),
                                fieldWithPath("[].isGlobal")
                                        .description("Is a global project?"),
                                fieldWithPath("[].students")
                                        .description("Students of the project"))));
    }

    @Test
    public void anonymizeProjectsBadRequest() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        List<Long> projectIds = new ArrayList<>();
        projectIds.add(projectId);

        Map<String, Object> dto = new HashMap<>();
        dto.put("anonymization_mode","YEPA");
        dto.put("project_ids", projectIds);


        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/anonymize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andDo(document("projects/anonymize/error-bad-request",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    @Test
    public void anonymizeProjectsConflict() throws Exception {
        AnonymizationModes mode = AnonymizationModes.GREEK_ALPHABET;

        Long projectId = 1L;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE,false);

        ObjectWriter objectWriter = mapper.writer().withDefaultPrettyPrinter();

        List<Long> projectIds = new ArrayList<>();
        projectIds.add(projectId);

        Map<String, Object> dto = new HashMap<>();
        dto.put("anonymization_mode", mode.toString());
        dto.put("project_ids", projectIds);

        when(projectsDomainController.anonymizeProjects(any(), any())).thenThrow(ProjectAlreadyAnonymizedException.class);


        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/api/projects/anonymize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectWriter.writeValueAsString(dto));

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isConflict())
                .andDo(document("projects/anonymize/error-conflict",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }
}