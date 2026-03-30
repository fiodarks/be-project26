package com.github.fiodarks.project26.users.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.api.AdministrationApi;
import com.github.fiodarks.project26.adapter.in.web.dto.AuditAction;
import com.github.fiodarks.project26.adapter.in.web.dto.AuditEventsPageResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.BlockUserCommand;
import com.github.fiodarks.project26.adapter.in.web.dto.SetUserRolesCommand;
import com.github.fiodarks.project26.adapter.in.web.dto.UserAdminDetailsResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.UserRole;
import com.github.fiodarks.project26.adapter.in.web.dto.UsersPageResponse;
import com.github.fiodarks.project26.audit.application.port.in.ListAuditEventsQuery;
import com.github.fiodarks.project26.audit.application.port.in.ListAuditEventsRequest;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventCommand;
import com.github.fiodarks.project26.audit.application.port.in.RecordAuditEventUseCase;
import com.github.fiodarks.project26.archive.adapter.in.web.security.SpringSecurityActorResolver;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.adapter.in.web.mapper.AuditAdminWebMapper;
import com.github.fiodarks.project26.users.adapter.in.web.mapper.UserAdminWebMapper;
import com.github.fiodarks.project26.users.application.port.in.ListUsersAdminRequest;
import com.github.fiodarks.project26.users.application.port.in.UserAdminDetails;
import com.github.fiodarks.project26.users.application.service.UserAccountApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Validated
public class AdministrationController implements AdministrationApi {

    private final SpringSecurityActorResolver actorResolver;
    private final UserAccountApplicationService users;
    private final ListAuditEventsQuery auditQuery;
    private final RecordAuditEventUseCase auditRecorder;

    @Override
    public ResponseEntity<UsersPageResponse> usersGet(Integer page, Integer size, String q, UserRole role, Boolean blocked) {
        var actor = actorResolver.requireActor();
        var domainRole = role == null ? null : Role.valueOf(role.name());
        var result = users.listUsers(actor, new ListUsersAdminRequest(
                page == null ? 0 : page,
                size == null ? 50 : size,
                q,
                domainRole,
                blocked
        ));
        return ResponseEntity.ok(UserAdminWebMapper.toUsersPageResponse(result));
    }

    @Override
    public ResponseEntity<Void> usersUserIdBlockPost(UUID userId, BlockUserCommand blockUserCommand) {
        var actor = actorResolver.requireActor();
        Objects.requireNonNull(blockUserCommand, "blockUserCommand");
        users.blockUser(
                actor,
                new UserId(userId),
                blockUserCommand.getBlockedUntil(),
                blockUserCommand.getReason()
        );
        auditRecorder.record(new RecordAuditEventCommand(
                actor,
                com.github.fiodarks.project26.audit.domain.model.AuditAction.USER_BLOCKED,
                new UserId(userId),
                null,
                blockUserCommand.getReason(),
                Map.of("blockedUntil", String.valueOf(blockUserCommand.getBlockedUntil()))
        ));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> usersUserIdBlockDelete(UUID userId) {
        var actor = actorResolver.requireActor();
        users.unblockUser(actor, new UserId(userId));
        auditRecorder.record(new RecordAuditEventCommand(
                actor,
                com.github.fiodarks.project26.audit.domain.model.AuditAction.USER_UNBLOCKED,
                new UserId(userId),
                null,
                null,
                null
        ));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> usersUserIdRolesPut(UUID userId, SetUserRolesCommand setUserRolesCommand) {
        var actor = actorResolver.requireActor();
        Objects.requireNonNull(setUserRolesCommand, "setUserRolesCommand");
        var roles = toRoles(setUserRolesCommand.getRoles());
        users.setUserRoles(actor, new UserId(userId), roles);
        auditRecorder.record(new RecordAuditEventCommand(
                actor,
                com.github.fiodarks.project26.audit.domain.model.AuditAction.USER_ROLES_CHANGED,
                new UserId(userId),
                null,
                null,
                Map.of("roles", roles.stream().map(Enum::name).sorted().collect(Collectors.joining(",")))
        ));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserAdminDetailsResponse> usersUserIdGet(UUID userId) {
        var actor = actorResolver.requireActor();
        UserAdminDetails details = users.getUserAdminDetails(actor, new UserId(userId));
        return ResponseEntity.ok(UserAdminWebMapper.toUserAdminDetailsResponse(details));
    }

    @Override
    public ResponseEntity<AuditEventsPageResponse> auditEventsGet(
            Integer page,
            Integer size,
            UUID actorUserId,
            UUID targetUserId,
            AuditAction action,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        var actor = actorResolver.requireActor();
        var result = auditQuery.list(actor, new ListAuditEventsRequest(
                page == null ? 0 : page,
                size == null ? 50 : size,
                actorUserId == null ? null : new UserId(actorUserId),
                targetUserId == null ? null : new UserId(targetUserId),
                action == null ? null : com.github.fiodarks.project26.audit.domain.model.AuditAction.valueOf(action.name()),
                from,
                to
        ));
        return ResponseEntity.ok(AuditAdminWebMapper.toAuditEventsPageResponse(result));
    }

    private static Set<Role> toRoles(java.util.Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("roles is required");
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(UserRole::name)
                .map(AdministrationController::parseRole)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
    }
}
