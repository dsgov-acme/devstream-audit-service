package io.nuvalence.platform.audit.service.service;

import io.nuvalence.platform.audit.service.domain.AuditEventEntity;
import io.nuvalence.platform.audit.service.error.ApiException;
import io.nuvalence.platform.audit.service.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service layer to manage audit events.
 */
@Component
@Transactional
@RequiredArgsConstructor
public class AuditEventService {
    private final AuditEventRepository auditEventRepository;
    private final PubSubService pubSubService;

    private static void checkTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw ApiException.Builder.badRequest(
                    "The startTime cannot be greater than the endTime.");
        }
    }

    static PageRequest createPageable(
            Integer pageNumber, Integer pageSize, String sortOrder, String sortBy) {
        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(sortOrder);
            return PageRequest.of(pageNumber, pageSize, sortDirection, sortBy);
        } catch (IllegalArgumentException e) {
            throw ApiException.Builder.badRequest(e.getMessage());
        }
    }

    /**
     * Queries audit events from db.
     *
     * @param businessObjectType Type of business object.
     * @param businessObjectId   Unique identifier for a business object of the specified type.
     * @param startTime          Specifies a start time (inclusive) for filtering results to events which occurred at
     *                           or after the specified time.
     * @param endTime            Specifies an end time (exclusive)for filtering results to events which occurred before
     *                           the specified time.
     * @param pageNumber         Results page number.
     * @param pageSize           Results page size.
     * @param sortOrder          Controls whether results are returned in chronologically ascending or descending order.
     * @param sortBy             Specifies the field to sort results by.
     * @return page object containing db query results and pagination metadata
     */
    @Transactional(readOnly = true)
    public Page<AuditEventEntity> findAuditEvents(
            String businessObjectType,
            UUID businessObjectId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            Integer pageNumber,
            Integer pageSize,
            String sortOrder,
            String sortBy) {
        checkTimeRange(startTime, endTime);

        return auditEventRepository.findAll(
                businessObjectType,
                businessObjectId,
                startTime,
                endTime,
                createPageable(pageNumber, pageSize, sortOrder, sortBy));
    }

    /**
     * Creates an audit event for a specific entity.
     *
     * @param auditEvent audit event data
     * @return event identifier for the created event
     */
    public AuditEventEntity addAuditEvent(AuditEventEntity auditEvent) {
        return pubSubService.publish(auditEvent);
    }
}
