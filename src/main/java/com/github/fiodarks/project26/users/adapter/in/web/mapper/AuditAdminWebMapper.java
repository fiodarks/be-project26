package com.github.fiodarks.project26.users.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.AuditAction;
import com.github.fiodarks.project26.adapter.in.web.dto.AuditEventDTO;
import com.github.fiodarks.project26.adapter.in.web.dto.AuditEventsPageResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.Pagination;
import com.github.fiodarks.project26.audit.domain.model.AuditEvent;
import com.github.fiodarks.project26.commons.PageResult;

import java.util.Objects;

public final class AuditAdminWebMapper {
    private AuditAdminWebMapper() {
    }

    public static AuditEventsPageResponse toAuditEventsPageResponse(PageResult<AuditEvent> page) {
        Objects.requireNonNull(page, "page");

        var response = new AuditEventsPageResponse();
        response.setItems(page.items().stream().map(AuditAdminWebMapper::toDto).toList());

        var pagination = new Pagination();
        pagination.setPage(page.page());
        pagination.setSize(page.size());
        pagination.setTotalElements(toIntClamped(page.totalElements()));
        pagination.setTotalPages(page.totalPages());
        response.setPagination(pagination);
        return response;
    }

    private static AuditEventDTO toDto(AuditEvent event) {
        Objects.requireNonNull(event, "event");

        var dto = new AuditEventDTO();
        dto.setId(event.id().value());
        dto.setAt(event.at());
        dto.setAction(AuditAction.valueOf(event.action().name()));
        dto.setActorUserId(event.actorUserId().value());
        dto.setTargetUserId(event.targetUserId() == null ? null : event.targetUserId().value());
        dto.setMaterialId(event.materialId() == null ? null : event.materialId().value());
        dto.setReason(event.reason());
        dto.setDetails(event.details());
        return dto;
    }

    private static int toIntClamped(long value) {
        if (value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        return Integer.MAX_VALUE;
    }
}

