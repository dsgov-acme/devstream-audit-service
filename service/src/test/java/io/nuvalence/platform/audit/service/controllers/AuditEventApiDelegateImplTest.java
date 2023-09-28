package io.nuvalence.platform.audit.service.controllers;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.error.ApiException;
import io.nuvalence.platform.audit.service.generated.models.AuditEventRequest;
import io.nuvalence.platform.audit.service.mapper.AuditEventMapper;
import io.nuvalence.platform.audit.service.repository.AuditEventRepository;
import io.nuvalence.platform.audit.service.service.AuditEventService;
import io.nuvalence.platform.audit.service.utils.TestUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
// @WebMvcTest doesn't set up controller method level validations:
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Disabled
class AuditEventApiDelegateImplTest {

    private static final String BUSINESS_OBJECT_TYPE = "orders";
    private static final String SORT_BY = "type";
    private static final UUID BUSINESS_OBJECT_ID =
            UUID.fromString("1190241c-5eae-11ec-bf63-0242ac130002");
    private static final String LIST_EVENTS_PATH =
            "/api/v1/audit-events/orders/1190241c-5eae-11ec-bf63-0242ac130002/";
    private static final String ASC = "ASC";

    @Autowired private MockMvc mockMvc;

    // This is to avoid loading the DB connection for the real repository class:
    @MockBean private AuditEventRepository auditEventRepository;

    @MockBean private AuditEventService auditEventService;

    @MockBean private AuthorizationHandler authorizationHandler;

    @Mock private AuditEventMapper eventMapper;

    @Test
    void getEvents() throws Exception {
        AuditEventEntity auditEvent = TestUtil.Data.STATE_CHANGE_ENTITY.readJson();
        Page<AuditEventEntity> page =
                new PageImpl<>(List.of(auditEvent), Pageable.ofSize(10).withPage(0), 20);
        // Expected result from PagingMetadataMapper.
        String nextPage =
                "http://localhost/api/v1/audit-events/orders/"
                        + "1190241c-5eae-11ec-bf63-0242ac130002/?sortOrder=ASC&pageSize=10&sortBy=type"
                        + "&startTime=2021-12-02T20:00:28.570Z&endTime=2021-12-22T20:00:28.570Z&pageNumber=1";

        when(auditEventService.findAuditEvents(
                        BUSINESS_OBJECT_TYPE,
                        BUSINESS_OBJECT_ID,
                        OffsetDateTime.parse("2021-12-02T20:00:28.570Z"),
                        OffsetDateTime.parse("2021-12-22T20:00:28.570Z"),
                        0,
                        10,
                        ASC,
                        SORT_BY))
                .thenReturn(page);

        String urlTemplate =
                LIST_EVENTS_PATH
                        + "?sortOrder=ASC&sortBy=type&startTime=2021-12-02T20:00:28.570Z&"
                        + "endTime=2021-12-22T20:00:28.570Z&"
                        + "pageNumber=0&pageSize=10";
        mockMvc.perform(get(urlTemplate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andExpect(jsonPath("$.events[0].summary").value("sed ipsum in ex"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(10))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(20))
                .andExpect(jsonPath("$.pagingMetadata.nextPage").value(nextPage));
    }

    @Test
    void getEvents_GivenInvalidTimeRange_ShouldReturnHttp400() throws Exception {
        when(auditEventService.findAuditEvents(
                        BUSINESS_OBJECT_TYPE,
                        BUSINESS_OBJECT_ID,
                        OffsetDateTime.parse("2021-12-02T20:00:28.571Z"),
                        OffsetDateTime.parse("2021-12-22T20:00:28.570Z"),
                        0,
                        10,
                        ASC,
                        SORT_BY))
                .thenThrow(ApiException.Builder.badRequest("ErRoR"));

        String urlTemplate =
                LIST_EVENTS_PATH
                        + "?sortOrder=ASC&sortBy=type&startTime=2021-12-02T20:00:28.571Z&"
                        + "endTime=2021-12-22T20:00:28.570Z&"
                        + "pageNumber=0&pageSize=10";
        mockMvc.perform(get(urlTemplate))
                .andExpect(status().isBadRequest())
                .andExpect(correctErrorMessages("ErRoR"));
    }

    @Test
    void getEvents_GivenTooLowPageNumber_ShouldReturnHttp400() throws Exception {
        String urlTemplate =
                LIST_EVENTS_PATH
                        + "?sortOrder=ASC&startTime=2021-12-02T20:00:28.571Z&endTime=2021-12-22T20:00:28.572Z&"
                        + "pageNumber=-1&pageSize=10";
        mockMvc.perform(get(urlTemplate))
                .andExpect(status().isBadRequest())
                .andExpect(correctErrorMessages("pageNumber: must be greater than or equal to 0"));
    }

    private static ResultMatcher correctErrorMessages(String... errorMessages) {
        return jsonPath("$.messages", hasItems(errorMessages));
    }

    @Test
    void getEvents_GivenTooLowPageSize_ShouldReturnHttp400() throws Exception {
        String urlTemplate =
                LIST_EVENTS_PATH
                        + "?sortOrder=ASC&startTime=2021-12-02T20:00:28.571Z&endTime=2021-12-22T20:00:28.572Z&"
                        + "pageNumber=0&pageSize=0";
        mockMvc.perform(get(urlTemplate))
                .andExpect(status().isBadRequest())
                .andExpect(correctErrorMessages("pageSize: must be greater than or equal to 1"));
    }

    @Test
    void getEvents_GivenTooHighPageSize_ShouldReturnHttp400() throws Exception {
        String urlTemplate =
                LIST_EVENTS_PATH
                        + "?sortOrder=ASC&startTime=2021-12-02T20:00:28.571Z&endTime=2021-12-22T20:00:28.572Z&"
                        + "pageNumber=0&pageSize=201";
        mockMvc.perform(get(urlTemplate))
                .andExpect(status().isBadRequest())
                .andExpect(correctErrorMessages("pageSize: must be less than or equal to 200"));
    }

    @Test
    void postEvent() throws Exception {
        String eventId = "6950bc28-4c09-43fe-8361-2a26555e92b6";

        AuditEventRequest body = TestUtil.Data.STATE_CHANGE_REQUEST.readJson();
        AuditEventEntity bodyData = TestUtil.Data.STATE_CHANGE_ENTITY.readJson();

        // Mock publishing this event by assigning an ID.
        AuditEventEntity result = TestUtil.Data.STATE_CHANGE_ENTITY.readJson();
        result.setEventId(UUID.fromString(eventId));

        when(eventMapper.toEntity(BUSINESS_OBJECT_ID, BUSINESS_OBJECT_TYPE, body))
                .thenReturn(bodyData);
        when(auditEventService.addAuditEvent(any())).thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/audit-events/orders/1190241c-5eae-11ec-bf63-0242ac130002/")
                                .content(TestUtil.Data.STATE_CHANGE_REQUEST.readJsonString())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId));
    }

    @Test
    void postEvent_GivenInvalidRequestBody_ShouldReturnHttp400() throws Exception {
        mockMvc.perform(
                        post("/api/v1/audit-events/orders/1190241c-5eae-11ec-bf63-0242ac130002/")
                                .content("{}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(
                        correctErrorMessages(
                                "'eventData': must not be null",
                                "'timestamp': must not be null",
                                "'summary': must not be null"));
    }
}
