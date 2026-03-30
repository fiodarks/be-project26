package com.github.fiodarks.project26.audit.application.service;

import com.github.fiodarks.project26.audit.application.port.in.ListAuditEventsQuery;
import com.github.fiodarks.project26.audit.application.port.in.ListAuditEventsRequest;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventCommand;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventUseCase;
import com.github.fiodarks.project26.audit.application.port.out.AuditEventRepositoryPort;
import com.github.fiodarks.project26.audit.application.port.out.AuditEventSearchCriteria;
import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.audit.domain.model.AuditEventId;
import com.github.fiodarks.project26.archive.application.exception.ForbiddenOperationException;
import com.github.fiodarks.project26.commons.PageResult;
import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditApplicationService implements ListAuditEventsQuery, RecordAuditEventUseCase {

    private final AuditEventRepositoryPort repository;
    private final Clock clock;

    @Override
    public void record(RecordAuditEventCommand command) {
        Objects.requireNonNull(command, "command");

        var now = OffsetDateTime.now(clock);
        var event = new AuditEvent(
                new AuditEventId(UUID.randomUUID()),
                now,
                command.action(),
                command.actor().userId(),
                command.targetUserId(),
                command.materialId(),
                command.reason(),
                command.details()
        );
        repository.save(event);
    }

    @Override
    public PageResult<AuditEvent> list(Actor actor, ListAuditEventsRequest request) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(request, "request");
        requireAdmin(actor);

        return repository.search(new AuditEventSearchCriteria(
                request.page(),
                request.size(),
                request.actorUserId(),
                request.targetUserId(),
                request.action(),
                request.from(),
                request.to()
        ));
    }

    private static void requireAdmin(Actor actor) {
        if (!actor.has(Role.ADMIN)) {
            throw new ForbiddenOperationException("Administrator role required");
        }
    }
}

