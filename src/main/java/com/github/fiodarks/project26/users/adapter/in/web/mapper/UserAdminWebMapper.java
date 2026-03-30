package com.github.fiodarks.project26.users.adapter.in.web.mapper;

import com.github.fiodarks.project26.adapter.in.web.dto.Pagination;
import com.github.fiodarks.project26.adapter.in.web.dto.UserAdminDetailsResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.UserAdminSummaryDTO;
import com.github.fiodarks.project26.adapter.in.web.dto.UserRole;
import com.github.fiodarks.project26.adapter.in.web.dto.UsersPageResponse;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.application.port.in.UserAdminDetails;
import com.github.fiodarks.project26.users.application.port.in.UserAdminSummary;
import com.github.fiodarks.project26.users.application.port.in.UsersAdminPage;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public final class UserAdminWebMapper {
    private UserAdminWebMapper() {
    }

    public static UsersPageResponse toUsersPageResponse(UsersAdminPage page) {
        Objects.requireNonNull(page, "page");

        var response = new UsersPageResponse();
        response.setItems(page.page().items().stream()
                .map(UserAdminWebMapper::toSummaryDto)
                .toList());

        var pagination = new Pagination();
        pagination.setPage(page.page().page());
        pagination.setSize(page.page().size());
        pagination.setTotalElements(toIntClamped(page.page().totalElements()));
        pagination.setTotalPages(page.page().totalPages());
        response.setPagination(pagination);
        return response;
    }

    public static UserAdminDetailsResponse toUserAdminDetailsResponse(UserAdminDetails details) {
        Objects.requireNonNull(details, "details");

        var dto = new UserAdminDetailsResponse();
        dto.setUserId(details.userId().value());
        dto.setEmail(details.email());
        dto.setName(details.name());
        dto.setSurname(details.surname());
        dto.setRoles(details.roles().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Role::name))
                .map(r -> UserRole.valueOf(r.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        dto.setBlockedUntil(details.blockedUntil());
        dto.setBlockedReason(details.blockedReason());
        dto.setCreatedAt(details.createdAt());
        dto.setLastLoginAt(details.lastLoginAt());
        dto.setMaterialsCount(details.materialsCount() == null ? null : toIntClamped(details.materialsCount()));
        dto.setLastMaterialCreatedAt(details.lastMaterialCreatedAt());
        dto.setLastModerationAt(details.lastModerationAt());
        dto.setStrikesCount(details.strikesCount());
        return dto;
    }

    private static UserAdminSummaryDTO toSummaryDto(UserAdminSummary summary) {
        Objects.requireNonNull(summary, "summary");
        var dto = new UserAdminSummaryDTO();
        dto.setUserId(summary.userId().value());
        dto.setEmail(summary.email());
        dto.setName(summary.name());
        dto.setSurname(summary.surname());
        dto.setRoles(summary.roles().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Role::name))
                .map(r -> UserRole.valueOf(r.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        dto.setBlockedUntil(summary.blockedUntil());
        dto.setBlockedReason(summary.blockedReason());
        dto.setCreatedAt(summary.createdAt());
        dto.setLastLoginAt(summary.lastLoginAt());
        dto.setMaterialsCount(summary.materialsCount() == null ? null : toIntClamped(summary.materialsCount()));
        dto.setLastMaterialCreatedAt(summary.lastMaterialCreatedAt());
        return dto;
    }

    private static int toIntClamped(long value) {
        if (value <= Integer.MAX_VALUE) {
            return (int) value;
        }
        return Integer.MAX_VALUE;
    }
}
