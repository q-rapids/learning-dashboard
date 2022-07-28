package com.upc.gessi.qrapids.app.presentation.rest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.upc.gessi.qrapids.app.domain.controllers.IterationsController;
import com.upc.gessi.qrapids.app.domain.models.IterationAPIBody;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOIteration;
import com.upc.gessi.qrapids.app.presentation.rest.dto.DTOStudent;
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
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IterationTest {

    private DomainObjectsBuilder domainObjectsBuilder;

    private MockMvc mockMvc;

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Mock
    private IterationsController iterationsDomainController;

    @InjectMocks
    private Iterations iterationsRestController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(iterationsRestController)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        domainObjectsBuilder = new DomainObjectsBuilder();
    }

    @Test
    public void getIterations() throws Exception {

        String url = "/api/iterations";
        DTOIteration expectedIteration = domainObjectsBuilder.buildIteration();
        List<DTOIteration> iterations = new ArrayList<>();
        iterations.add(expectedIteration);

        //Given
        when(iterationsDomainController.getAllIterations()).thenReturn(iterations);

        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(url);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(expectedIteration.getId().intValue())))
                .andExpect(jsonPath("$[0].name", is(expectedIteration.getName())))
                .andExpect(jsonPath("$[0].label", is(expectedIteration.getLabel())))
                .andExpect(jsonPath("$[0].from_date", is(expectedIteration.getFrom_date().getTime())))
                .andExpect(jsonPath("$[0].to_date", is(expectedIteration.getTo_date().getTime())))
                .andExpect(jsonPath("$[0].project_ids.size()", is(expectedIteration.getProject_ids().size())))
                .andDo(document("iterations/all",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id").description("Iteration identifier"),
                                fieldWithPath("[].name").description("Iteration name"),
                                fieldWithPath("[].label").description("(Optional) Iteration label"),
                                fieldWithPath("[].from_date").description("Iteration start date"),
                                fieldWithPath("[].to_date").description("Iteration end date"),
                                fieldWithPath("[].project_ids").description("Project associated with the iteration")
                        )
                ));

        // Verify mock interactions
        verify(iterationsDomainController, times(1)).getAllIterations();
        verifyNoMoreInteractions(iterationsDomainController);
    }

    @Test
    public void updateIterations() throws Exception {
        //given

        Map<String, String> iteration = new HashMap<>();
        iteration.put("fromDate", "2022-02-25");
        iteration.put("toDate", "2022-05-15");
        iteration.put("name", "test iteration");
        iteration.put("label", "test label");

        List<Long> project_ids = new ArrayList<>();
        project_ids.add(2L);
        project_ids.add(4L);

        IterationAPIBody requestBody = new IterationAPIBody(project_ids, iteration);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        String requestJson=ow.writeValueAsString(requestBody);

        /*
        // Perform request
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .multipart("/api/iterations/{iteration_id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(new RequestPostProcessor() {
                    @Override
                    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
                        request.setMethod("PUT");
                        return request;
                    }
                });

         */

        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .put("/api/iterations/{iteration_id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson);

        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("iterations/update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("iteration_id")
                                        .description("Iteration identifier to be modified")
                        ),
                        requestFields(
                                fieldWithPath("iteration.name").description("New iteration name"),
                                fieldWithPath("iteration.label").description("(Optional) New iteration label"),
                                fieldWithPath("iteration.fromDate").description("New iteration start date"),
                                fieldWithPath("iteration.toDate").description("New iteration end date"),
                                fieldWithPath("project_ids").description("New projects associated with the iteration")
                        )
                ));

        // Verify mock interactions
        ArgumentCaptor<Map<String, String>> argument1 = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List<Long>> argument2 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> argument3 = ArgumentCaptor.forClass(Long.class);

        verify(iterationsDomainController, times(1)).updateIteration(argument1.capture(), argument2.capture(),argument3.capture());

        assertEquals(iteration.get("fromDate"), argument1.getValue().get("fromDate"));
        assertEquals(iteration.get("toDate"), argument1.getValue().get("toDate"));
        assertEquals(iteration.get("name"), argument1.getValue().get("name"));
        assertEquals(iteration.get("label"), argument1.getValue().get("label"));

        assertEquals(project_ids, argument2.getValue());

        verifyNoMoreInteractions(iterationsDomainController);
    }

    @Test
    public void deleteIterations() throws Exception {
        //Given
        RequestBuilder requestBuilder = RestDocumentationRequestBuilders
                .delete("/api/iterations/{iteration_id}", 1L);

        // Perform request
        this.mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andDo(document("iterations/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("iteration_id")
                                        .description("Iteration identifier")
                        )
                ));

        // Verify mock interactions
        verify(iterationsDomainController, times(1)).deleteIteration(1L);
        verifyNoMoreInteractions(iterationsDomainController);
    }


}
