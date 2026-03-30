package com.github.fiodarks.project26.audit.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AuditEventId(UUID value) {
    public AuditEventId {
        Objects.requireNonNull(value, "value");
    }
}

