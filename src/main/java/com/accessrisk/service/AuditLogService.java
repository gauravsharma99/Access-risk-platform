package com.accessrisk.service;

import com.accessrisk.dto.response.AuditLogResponse;
import com.accessrisk.entity.AuditLog;
import com.accessrisk.enums.AuditAction;
import com.accessrisk.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final AuditLogRepository auditLogRepository;

    /**
     * Records a system-initiated audit event (e.g. automated risk analysis).
     * Uses REQUIRES_NEW so audit writes always commit independently of the
     * caller's transaction. If the caller rolls back, the audit entry survives —
     * which is correct: you want to know that an operation was *attempted* even
     * if it ultimately failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, Long entityId, String details) {
        persist(action, entityType, entityId, details, SYSTEM_ACTOR);
    }

    /**
     * Records a user-initiated audit event with an explicit actor (e.g. the
     * authenticated user's email). Use this overload from user-facing API operations.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, Long entityId,
                    String details, String createdBy) {
        persist(action, entityType, entityId, details, createdBy);
    }

    /**
     * Returns all audit log entries, most-recent-first, paginated.
     * Never expose an unbounded findAll — audit tables grow to millions of rows.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(AuditLogResponse::from);
    }

    /**
     * Returns the full history for a specific entity instance.
     * Useful for "show me everything that happened to this user" views.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void persist(AuditAction action, String entityType, Long entityId,
                         String details, String createdBy) {
        AuditLog entry = AuditLog.builder()
                .action(action.name())   // stored as String — enum name is stable
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdBy(createdBy)
                .build();

        auditLogRepository.save(entry);

        log.debug("AUDIT [{}] entity={} id={} by={} | {}",
                action, entityType, entityId, createdBy, details);
    }
}
