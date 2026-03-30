package com.github.fiodarks.project26.audit.application.port.in;

import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.commons.PageResult;
import com.github.fiodarks.project26.security.Actor;

public interface ListAuditEventsQuery {
    PageResult<AuditEvent> list(Actor actor, ListAuditEventsRequest request);
}

