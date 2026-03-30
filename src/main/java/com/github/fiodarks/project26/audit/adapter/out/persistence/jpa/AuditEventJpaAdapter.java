package com.github.fiodarks.project26.audit.adapter.out.persistence.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.mapper.AuditEventPersistenceMapper;
import com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.repository.AuditEventSpringDataRepository;
import com.github.fiodarks.project26.audit.application.port.out.AuditEventRepositoryPort;
import com.github.fiodarks.project26.audit.application.port.out.AuditEventSearchCriteria;
import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.commons.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AuditEventJpaAdapter implements AuditEventRepositoryPort {

    private final AuditEventSpringDataRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AuditEvent save(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        var saved = repository.save(AuditEventPersistenceMapper.toEntity(objectMapper, event));
        return AuditEventPersistenceMapper.toDomain(objectMapper, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditEvent> search(AuditEventSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");

        var pageRequest = PageRequest.of(criteria.page(), criteria.size());
        var page = repository.search(
                criteria.actorUserId() == null ? null : criteria.actorUserId().value(),
                criteria.targetUserId() == null ? null : criteria.targetUserId().value(),
                criteria.action() == null ? null : criteria.action().name(),
                criteria.from(),
                criteria.to(),
                pageRequest
        );

        return new PageResult<>(
                page.getContent().stream()
                        .map(e -> AuditEventPersistenceMapper.toDomain(objectMapper, e))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

