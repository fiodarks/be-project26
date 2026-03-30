package com.github.fiodarks.project26.audit.domain.model;

import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(
        AuditEventId id,
        OffsetDateTime at,
        AuditAction action,
        UserId actorUserId,
        UserId targetUserId,
        MaterialId materialId,
        String reason,
        Map<String, String> details
) {
    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorUserId, "actorUserId");
        if (details != null) {
            details = Map.copyOf(details);
        }
    }
}

