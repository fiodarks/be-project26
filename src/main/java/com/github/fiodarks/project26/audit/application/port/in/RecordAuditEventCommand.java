package com.github.fiodarks.project26.audit.application.port.in;

import com.github.fiodarks.project26.audit.domain.model.AuditAction;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Actor;

import java.util.Map;
import java.util.Objects;

public record RecordAuditEventCommand(
        Actor actor,
        AuditAction action,
        UserId targetUserId,
        MaterialId materialId,
        String reason,
        Map<String, String> details
) {
    public RecordAuditEventCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        if (details != null) {
            details = Map.copyOf(details);
        }
    }
}

