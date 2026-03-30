package com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fiodarks.project26.audit.adapter.out.persistence.jpa.entity.AuditEventJpaEntity;
import com.github.fiodarks.project26.audit.domain.model.AuditAction;
import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.audit.domain.model.AuditEventId;
import com.github.fiodarks.project26.archive.domain.model.MaterialId;
import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.util.Map;
import java.util.Objects;

public final class AuditEventPersistenceMapper {
    private static final TypeReference<Map<String, String>> DETAILS_TYPE = new TypeReference<>() {
    };

    private AuditEventPersistenceMapper() {
    }

    public static AuditEventJpaEntity toEntity(ObjectMapper objectMapper, AuditEvent event) {
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(event, "event");

        var entity = new AuditEventJpaEntity();
        entity.setId(event.id().value());
        entity.setAt(event.at());
        entity.setAction(event.action().name());
        entity.setActorUserId(event.actorUserId().value());
        entity.setTargetUserId(event.targetUserId() == null ? null : event.targetUserId().value());
        entity.setMaterialId(event.materialId() == null ? null : event.materialId().value());
        entity.setReason(event.reason());
        entity.setDetailsJson(serializeDetails(objectMapper, event.details()));
        return entity;
    }

    public static AuditEvent toDomain(ObjectMapper objectMapper, AuditEventJpaEntity entity) {
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(entity, "entity");

        return new AuditEvent(
                new AuditEventId(entity.getId()),
                entity.getAt(),
                AuditAction.valueOf(entity.getAction()),
                new UserId(entity.getActorUserId()),
                entity.getTargetUserId() == null ? null : new UserId(entity.getTargetUserId()),
                entity.getMaterialId() == null ? null : new MaterialId(entity.getMaterialId()),
                entity.getReason(),
                deserializeDetails(objectMapper, entity.getDetailsJson())
        );
    }

    private static String serializeDetails(ObjectMapper objectMapper, Map<String, String> details) {
        if (details == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize audit event details", e);
        }
    }

    private static Map<String, String> deserializeDetails(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DETAILS_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize audit event details", e);
        }
    }
}

