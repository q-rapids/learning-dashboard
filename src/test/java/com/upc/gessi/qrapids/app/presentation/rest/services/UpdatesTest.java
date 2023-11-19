package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.upc.gessi.qrapids.app.domain.controllers.UpdatesController;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOProject;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOUpdate;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UpdatesTest {

    private DomainObjectsBuilder domainObjectsBuilder;

    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Mock
    private UpdatesController updatesDomainController;

    @InjectMocks
    private Updates updatesController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(updatesController)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getAllUpdate () throws Exception {

        List<DTOUpdate> allUpdatesList = new ArrayList<>();
        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, LocalDate.parse(date), update);
        allUpdatesList.add(dtoUpdate);

        // Given
        when(updatesDomainController.getUpdateList()).thenReturn(allUpdatesList);
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/update");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(name)))
                .andExpect(jsonPath("$[0].date[0]", is(localDate.getYear())))
                .andExpect(jsonPath("$[0].date[1]", is(localDate.getMonthValue())))
                .andExpect(jsonPath("$[0].date[2]", is(localDate.getDayOfMonth())))
                .andExpect(jsonPath("$[0].update", is(update)))
                .andDo(document("updates/all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Update identifier"),
                                fieldWithPath("[].name")
                                        .description("Update name"),
                                fieldWithPath("[].date")
                                        .description("Update date"),
                                fieldWithPath("[].update")
                                        .description("Description/List of updates and news")

                        )
                ));

        // Verify mock interactions
        verify(updatesDomainController, times(1)).getUpdateList();
        verifyNoMoreInteractions(updatesDomainController);
    }

    @Test
    public void getLastUpdate () throws Exception {

        List<DTOUpdate> allUpdatesList = new ArrayList<>();
        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, localDate, update);
        allUpdatesList.add(dtoUpdate);

        // Given
        when(updatesDomainController.getLastUpdate("test username")).thenReturn(allUpdatesList);
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/update/last")
                .param("username", "test username");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(name)))
                .andExpect(jsonPath("$[0].date[0]", is(localDate.getYear())))
                .andExpect(jsonPath("$[0].date[1]", is(localDate.getMonthValue())))
                .andExpect(jsonPath("$[0].date[2]", is(localDate.getDayOfMonth())))
                .andExpect(jsonPath("$[0].update", is(update)))
                .andDo(document("updates/last",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("username")
                                        .description("Connected user name")
                        ),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Update identifier"),
                                fieldWithPath("[].name")
                                        .description("Update name"),
                                fieldWithPath("[].date")
                                        .description("Update date"),
                                fieldWithPath("[].update")
                                        .description("Description/List of updates and news")

                        )
                ));

        // Verify mock interactions
        verify(updatesDomainController, times(1)).getLastUpdate("test username");
        verifyNoMoreInteractions(updatesDomainController);
    }

    @Test
    public void getLastYearUpdates () throws Exception {

        List<DTOUpdate> allUpdatesList = new ArrayList<>();
        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, localDate, update);
        allUpdatesList.add(dtoUpdate);

        // Given
        when(updatesDomainController.getUpdatesOfYear()).thenReturn(allUpdatesList);
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/api/update/year");

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(name)))
                .andExpect(jsonPath("$[0].date[0]", is(localDate.getYear())))
                .andExpect(jsonPath("$[0].date[1]", is(localDate.getMonthValue())))
                .andExpect(jsonPath("$[0].date[2]", is(localDate.getDayOfMonth())))
                .andExpect(jsonPath("$[0].update", is(update)))
                .andDo(document("updates/year",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id")
                                        .description("Update identifier"),
                                fieldWithPath("[].name")
                                        .description("Update name"),
                                fieldWithPath("[].date")
                                        .description("Update date"),
                                fieldWithPath("[].update")
                                        .description("Description/List of updates and news")

                        )
                ));

        // Verify mock interactions
        verify(updatesDomainController, times(1)).getUpdatesOfYear();
        verifyNoMoreInteractions(updatesDomainController);
    }

    @Test
    public void getUpdate () throws Exception {

        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, localDate, update);

        // Given
        when(updatesDomainController.getUpdateById(1L)).thenReturn(dtoUpdate);
        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .get("/api/update/{id}", 1L);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.date[0]", is(localDate.getYear())))
                .andExpect(jsonPath("$.date[1]", is(localDate.getMonthValue())))
                .andExpect(jsonPath("$.date[2]", is(localDate.getDayOfMonth())))
                .andExpect(jsonPath("$.update", is(update)))
                .andDo(document("updates/single",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Update identifier")
                        ),
                        responseFields(
                                fieldWithPath("id")
                                        .description("Update identifier"),
                                fieldWithPath("name")
                                        .description("Update name"),
                                fieldWithPath("date")
                                        .description("Update date"),
                                fieldWithPath("update")
                                        .description("Description/List of updates and news")

                        )
                ));

        // Verify mock interactions
        verify(updatesDomainController, times(1)).getUpdateById(1L);
        verifyNoMoreInteractions(updatesDomainController);
    }

    @Test
    public void createUpdate() throws Exception {

        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, localDate, update);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/update")
                .param("name", name)
                .param("date", date)
                .param("update", update)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("POST");
                        return request;
                    }
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isCreated())
                .andDo(document("updates/new",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParameters(
                                parameterWithName("name")
                                        .description("Update name"),
                                parameterWithName("date")
                                        .description("Update date, in format yyyy-MM-dd"),
                                parameterWithName("update")
                                        .description("Description/List of updates and news")
                        )
                ));

        // Verify mock interactions
        ArgumentCaptor<DTOUpdate> argument = ArgumentCaptor.forClass(DTOUpdate.class);
        verify(updatesDomainController, times(1)).newUpdate(argument.capture());
        assertEquals(dtoUpdate.getName(), argument.getValue().getName());
        assertEquals(dtoUpdate.getDate(), argument.getValue().getDate());
        assertEquals(dtoUpdate.getUpdate(), argument.getValue().getUpdate());
        verifyNoMoreInteractions(updatesDomainController);


    }

    @Test
    public void updateUpdate() throws Exception {

        String date = "2000-01-01";
        String name = "Test";
        String update = "Test update";
        LocalDate localDate = LocalDate.parse(date);
        DTOUpdate dtoUpdate = new DTOUpdate(1L, name, localDate, update);

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .put("/api/update/{id}", 1L)
                .param("name", name)
                .param("date", date)
                .param("update", update)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("updates/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Update identifier")
                        ),
                        requestParameters(
                                parameterWithName("name")
                                        .description("Update identifier"),
                                parameterWithName("date")
                                        .description("Update date, in format yyyy-MM-dd"),
                                parameterWithName("update")
                                        .description("Description/List of updates and news")
                        )
                ));

        // Verify mock interactions
        ArgumentCaptor<DTOUpdate> argument = ArgumentCaptor.forClass(DTOUpdate.class);
        verify(updatesDomainController, times(1)).updateUpdateById(argument.capture());
        assertEquals(dtoUpdate.getId(), argument.getValue().getId());
        assertEquals(dtoUpdate.getName(), argument.getValue().getName());
        assertEquals(dtoUpdate.getDate(), argument.getValue().getDate());
        assertEquals(dtoUpdate.getUpdate(), argument.getValue().getUpdate());
        verifyNoMoreInteractions(updatesDomainController);


    }

    @Test
    public void deleteUpdate() throws Exception {

        // Perform request
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .delete("/api/update/{id}", 1L);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("updates/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id")
                                        .description("Update identifier")
                        )
                ));

        // Verify mock interactions
        verify(updatesDomainController, times(1)).deleteUpdateById(1L);
        verifyNoMoreInteractions(updatesDomainController);
    }



}
