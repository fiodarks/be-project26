package com.github.fiodarks.project26.audit.application.port.out;

import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.commons.PageResult;

public interface AuditEventRepositoryPort {
    AuditEvent save(AuditEvent event);

    PageResult<AuditEvent> search(AuditEventSearchCriteria criteria);
}

