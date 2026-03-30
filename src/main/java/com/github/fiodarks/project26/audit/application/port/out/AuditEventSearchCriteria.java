package com.github.fiodarks.project26.audit.application.port.out;

import com.github.fiodarks.project26.audit.domain.model.AuditAction;
import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.time.OffsetDateTime;

public record AuditEventSearchCriteria(
        int page,
        int size,
        UserId actorUserId,
        UserId targetUserId,
        AuditAction action,
        OffsetDateTime from,
        OffsetDateTime to
) {
    public AuditEventSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be >= 1");
        }
    }
}

