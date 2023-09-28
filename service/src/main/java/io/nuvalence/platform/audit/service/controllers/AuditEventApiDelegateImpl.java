package io.nuvalence.platform.audit.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.generated.controllers.AuditEventsApiDelegate;
import io.nuvalence.platform.audit.service.generated.models.AuditEventId;
import io.nuvalence.platform.audit.service.generated.models.AuditEventRequest;
import io.nuvalence.platform.audit.service.generated.models.AuditEventsPage;
import io.nuvalence.platform.audit.service.mapper.AuditEventMapper;
import io.nuvalence.platform.audit.service.mapper.PagingMetadataMapper;
import io.nuvalence.platform.audit.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controller layer for audit service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditEventApiDelegateImpl implements AuditEventsApiDelegate {
    private final AuditEventService auditEventService;

    private final AuthorizationHandler authorizationHandler;

    private final PagingMetadataMapper pagingMetadataMapper;
    private final AuditEventMapper auditEventMapper;

    @Override
    public ResponseEntity<AuditEventsPage> getEvents(
            String businessObjectType,
            UUID businessObjectId,
            String sortOrder,
            String sortBy,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Integer pageNumber,
            Integer pageSize) {
        if (!authorizationHandler.isAllowed("view", AuditEventEntity.class)) {
            throw new AccessDeniedException("You do not have permission to view this resource.");
        }

        var events =
                auditEventService.findAuditEvents(
                        businessObjectType,
                        businessObjectId,
                        startTime,
                        endTime,
                        pageNumber,
                        pageSize,
                        sortOrder,
                        sortBy);

        return ResponseEntity.ok(this.createAuditEventsPage(events));
    }

    @Override
    public ResponseEntity<AuditEventId> postEvent(
            String businessObjectType, UUID businessObjectId, AuditEventRequest body) {
        if (!authorizationHandler.isAllowed("create", AuditEventEntity.class)) {
            throw new AccessDeniedException("You do not have permission to create this resource.");
        }

        log.debug(
                "Received audit event request for business object type {} and id {}",
                businessObjectType,
                businessObjectId);

        var eventBody = auditEventMapper.toEntity(businessObjectId, businessObjectType, body);
        var event = auditEventService.addAuditEvent(eventBody);
        var eventId = new AuditEventId().eventId(event.getEventId());

        return ResponseEntity.status(201).body(eventId);
    }

    private AuditEventsPage createAuditEventsPage(Page<AuditEventEntity> page) {
        return new AuditEventsPage()
                .events(auditEventMapper.fromEntities(page.getContent()))
                .pagingMetadata(pagingMetadataMapper.toPagingMetadata(page));
    }
}
