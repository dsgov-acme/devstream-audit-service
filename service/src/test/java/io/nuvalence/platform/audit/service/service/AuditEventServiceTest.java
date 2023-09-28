package io.nuvalence.platform.audit.service.service;

import static io.nuvalence.platform.audit.service.utils.TestUtil.Data.ACTIVITY_ENTITY;
import static io.nuvalence.platform.audit.service.utils.TestUtil.Data.STATE_CHANGE_ENTITY;

import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.error.ApiException;
import io.nuvalence.platform.audit.service.repository.AuditEventRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.BeanMembersShouldSerialize"})
@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {
    private static final Random random = new Random();
    private static final String DESC = "DESC";
    private final UUID businessObjectId = UUID.randomUUID();
    private final String businessObjectType = "business-object-type-" + UUID.randomUUID();
    @Mock private AuditEventRepository mockRepository;
    @Mock private PubSubService mockPubSubService;

    private AuditEventService service;

    @BeforeEach
    public void beforeEach() {
        service = new AuditEventService(mockRepository, mockPubSubService);
    }

    @Test
    void addAuditEvent_GivenBusinessObjectAndEventData_ShouldReturnMapToEntityPersistAndReturnId() {
        var expectedId = UUID.randomUUID();
        AuditEventEntity event = Mockito.mock(AuditEventEntity.class);
        Mockito.when(event.getEventId()).thenReturn(expectedId);
        Mockito.when(mockPubSubService.publish(event)).thenReturn(event);

        var result = service.addAuditEvent(event);

        Assertions.assertAll(
                () -> Mockito.verify(mockPubSubService).publish(event),
                () -> Assertions.assertEquals(expectedId, result.getEventId()));
    }

    @Test
    void findAuditEvents_GivenQueryParameters_ShouldReturnPagedResult() throws IOException {
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = OffsetDateTime.now();
        int pageNumber = random.nextInt(10);
        int pageSize = 1 + random.nextInt(100);
        String sortOrder = DESC;
        String sortBy = "type";
        PageRequest pageRequest =
                AuditEventService.createPageable(pageNumber, pageSize, sortOrder, sortBy);
        List<AuditEventEntity> eventsFromRepository =
                List.of(ACTIVITY_ENTITY.readJson(), STATE_CHANGE_ENTITY.readJson());

        PageImpl<AuditEventEntity> page = new PageImpl<>(eventsFromRepository);
        Mockito.when(
                        mockRepository.findAll(
                                businessObjectType,
                                businessObjectId,
                                startTime,
                                endTime,
                                pageRequest))
                .thenReturn(page);

        var actual =
                service.findAuditEvents(
                        businessObjectType,
                        businessObjectId,
                        startTime,
                        endTime,
                        pageNumber,
                        pageSize,
                        sortOrder,
                        sortBy);

        Assertions.assertAll(
                () ->
                        Mockito.verify(mockRepository)
                                .findAll(
                                        businessObjectType,
                                        businessObjectId,
                                        startTime,
                                        endTime,
                                        pageRequest),
                () -> Assertions.assertEquals(page, actual));
    }

    @Test
    void findAuditEvents_GivenInvalidTimeRangeParameters_ShouldThrowError() {
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = OffsetDateTime.now().minus(1, ChronoUnit.SECONDS);
        int pageNumber = random.nextInt(10);
        int pageSize = random.nextInt(100);
        String sortBy = "type";
        Assertions.assertThrows(
                ApiException.class,
                () ->
                        service.findAuditEvents(
                                businessObjectType,
                                businessObjectId,
                                startTime,
                                endTime,
                                pageNumber,
                                pageSize,
                                DESC,
                                sortBy));
    }

    @Test
    void findAuditEvents_GivenInvalidSortOrderParameter_ShouldThrowError() {
        OffsetDateTime startTime = OffsetDateTime.now();
        OffsetDateTime endTime = OffsetDateTime.now();
        int pageNumber = random.nextInt(10);
        int pageSize = random.nextInt(100);
        String sortBy = "type";
        Assertions.assertThrows(
                ApiException.class,
                () ->
                        service.findAuditEvents(
                                businessObjectType,
                                businessObjectId,
                                startTime,
                                endTime,
                                pageNumber,
                                pageSize,
                                "FOO",
                                sortBy));
    }
}
