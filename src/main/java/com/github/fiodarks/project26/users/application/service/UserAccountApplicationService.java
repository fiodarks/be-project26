package com.github.fiodarks.project26.users.application.service;

import com.github.fiodarks.project26.archive.application.exception.ForbiddenOperationException;
import com.github.fiodarks.project26.archive.application.exception.NotFoundException;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.config.ArchiveBootstrapAdminProperties;
import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.application.port.in.ListUsersAdminQuery;
import com.github.fiodarks.project26.users.application.port.in.ListUsersAdminRequest;
import com.github.fiodarks.project26.users.application.port.in.UserAdminSummary;
import com.github.fiodarks.project26.users.application.port.in.UsersAdminPage;
import com.github.fiodarks.project26.users.application.port.in.BlockUserUseCase;
import com.github.fiodarks.project26.users.application.port.in.GetCurrentUserQuery;
import com.github.fiodarks.project26.users.application.port.in.GetUserAccountQuery;
import com.github.fiodarks.project26.users.application.port.in.GetUserAdminDetailsQuery;
import com.github.fiodarks.project26.users.application.port.in.ResolveLoginRolesUseCase;
import com.github.fiodarks.project26.users.application.port.in.RegisterLocalUserCommand;
import com.github.fiodarks.project26.users.application.port.in.RegisterLocalUserUseCase;
import com.github.fiodarks.project26.users.application.port.in.SetUserRolesUseCase;
import com.github.fiodarks.project26.users.application.port.in.UnblockUserUseCase;
import com.github.fiodarks.project26.users.application.port.in.UserAdminDetails;
import com.github.fiodarks.project26.users.application.port.in.UserProfile;
import com.github.fiodarks.project26.users.application.port.out.UserAccountSearchCriteria;
import com.github.fiodarks.project26.users.application.port.out.UserAccountRepositoryPort;
import com.github.fiodarks.project26.users.application.port.out.UserMaterialsStatsPort;
import com.github.fiodarks.project26.users.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccountApplicationService implements
        ResolveLoginRolesUseCase,
        RegisterLocalUserUseCase,
        SetUserRolesUseCase,
        BlockUserUseCase,
        UnblockUserUseCase,
        GetCurrentUserQuery,
        ListUsersAdminQuery,
        GetUserAccountQuery,
        GetUserAdminDetailsQuery {

    private final UserAccountRepositoryPort repository;
    private final UserMaterialsStatsPort materialsStats;
    private final ArchiveBootstrapAdminProperties bootstrapAdmins;
    private final Clock clock;

    @Override
    public Set<Role> resolveRolesForLogin(UserId userId, UserProfile profile) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(profile, "profile");

        var now = OffsetDateTime.now(clock);
        var existing = repository.findById(userId).orElse(null);

        if (existing == null) {
            var roles = defaultRolesForNewUser(profile.email());
            var created = new UserAccount(
                    userId,
                    normalizeEmail(profile.email()),
                    profile.name(),
                    profile.surname(),
                    profile.pictureUrl(),
                    null,
                    roles,
                    null,
                    null,
                    now,
                    now,
                    0L
            );
            return repository.save(created).roles();
        }

        var roles = existing.roles();
        if (shouldBootstrapAdmin(profile.email())) {
            roles = union(roles, Set.of(Role.ADMIN));
        }
        if (roles.isEmpty()) {
            roles = Set.of(Role.CREATOR);
        }

        var updated = new UserAccount(
                existing.id(),
                normalizeEmail(profile.email()),
                profile.name(),
                profile.surname(),
                profile.pictureUrl(),
                existing.passwordHash(),
                roles,
                existing.blockedUntil(),
                existing.blockedReason(),
                existing.createdAt(),
                now,
                existing.version()
        );
        return repository.save(updated).roles();
    }

    @Override
    public UserAccount registerLocalUser(RegisterLocalUserCommand command) {
        Objects.requireNonNull(command, "command");
        var normalizedEmail = normalizeEmail(command.email());
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (repository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        var now = OffsetDateTime.now(clock);
        var roles = defaultRolesForNewUser(normalizedEmail);
        var created = new UserAccount(
                new UserId(java.util.UUID.randomUUID()),
                normalizedEmail,
                command.name().trim(),
                command.surname().trim(),
                null,
                command.passwordHash(),
                roles,
                null,
                null,
                now,
                now,
                0L
        );
        return repository.save(created);
    }

    public java.util.Optional<UserAccount> findByEmail(String email) {
        var normalized = normalizeEmail(email);
        if (normalized == null) {
            return java.util.Optional.empty();
        }
        return repository.findByEmail(normalized);
    }

    @Override
    public void setUserRoles(Actor actor, UserId userId, Set<Role> roles) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(roles, "roles");
        requireAdmin(actor);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("roles must not be empty");
        }

        var existing = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId.value()));

        var now = OffsetDateTime.now(clock);
        var updated = new UserAccount(
                existing.id(),
                existing.email(),
                existing.name(),
                existing.surname(),
                existing.pictureUrl(),
                existing.passwordHash(),
                Set.copyOf(roles),
                existing.blockedUntil(),
                existing.blockedReason(),
                existing.createdAt(),
                now,
                existing.version()
        );
        repository.save(updated);
    }

    @Override
    public void blockUser(Actor actor, UserId userId, OffsetDateTime blockedUntil, String reason) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(blockedUntil, "blockedUntil");
        Objects.requireNonNull(reason, "reason");
        requireAdmin(actor);

        var now = OffsetDateTime.now(clock);
        if (!blockedUntil.isAfter(now)) {
            throw new IllegalArgumentException("blockedUntil must be in the future");
        }

        var existing = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId.value()));

        var updated = new UserAccount(
                existing.id(),
                existing.email(),
                existing.name(),
                existing.surname(),
                existing.pictureUrl(),
                existing.passwordHash(),
                existing.roles(),
                blockedUntil,
                reason,
                existing.createdAt(),
                now,
                existing.version()
        );
        repository.save(updated);
    }

    @Override
    public void unblockUser(Actor actor, UserId userId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(userId, "userId");
        requireAdmin(actor);

        var existing = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId.value()));

        var now = OffsetDateTime.now(clock);
        var updated = new UserAccount(
                existing.id(),
                existing.email(),
                existing.name(),
                existing.surname(),
                existing.pictureUrl(),
                existing.passwordHash(),
                existing.roles(),
                null,
                null,
                existing.createdAt(),
                now,
                existing.version()
        );
        repository.save(updated);
    }

    @Override
    public UserAccount getOrCreateCurrentUser(Actor actor) {
        Objects.requireNonNull(actor, "actor");
        var now = OffsetDateTime.now(clock);
        var existing = repository.findById(actor.userId()).orElse(null);
        if (existing != null) {
            return existing;
        }

        var created = new UserAccount(
                actor.userId(),
                null,
                null,
                null,
                null,
                null,
                actor.roles().isEmpty() ? Set.of(Role.VIEWER) : actor.roles(),
                null,
                null,
                now,
                now,
                0L
        );
        return repository.save(created);
    }

    @Override
    public UsersAdminPage listUsers(Actor actor, ListUsersAdminRequest request) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(request, "request");
        requireAdmin(actor);

        var now = OffsetDateTime.now(clock);
        var result = repository.search(new UserAccountSearchCriteria(
                request.page(),
                request.size(),
                request.q(),
                request.role(),
                request.blocked(),
                now
        ));

        var userIds = result.items().stream()
                .map(UserAccount::id)
                .collect(Collectors.toUnmodifiableSet());

        var stats = materialsStats.findStatsByUserIds(userIds);
        var items = result.items().stream()
                .map(account -> {
                    var materialStats = stats.get(account.id());
                    return new UserAdminSummary(
                            account.id(),
                            account.email(),
                            account.name(),
                            account.surname(),
                            account.roles(),
                            account.blockedUntil(),
                            account.blockedReason(),
                            account.createdAt(),
                            account.updatedAt(),
                            materialStats == null ? null : materialStats.materialsCount(),
                            materialStats == null ? null : materialStats.lastMaterialCreatedAt()
                    );
                })
                .toList();

        return new UsersAdminPage(new com.github.fiodarks.project26.commons.PageResult<>(
                items,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        ));
    }

    @Override
    public UserAdminDetails getUserAdminDetails(Actor actor, UserId userId) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(userId, "userId");
        requireAdmin(actor);

        var account = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId.value()));

        var stats = materialsStats.findStatsByUserIds(Set.of(userId)).get(userId);

        return new UserAdminDetails(
                account.id(),
                account.email(),
                account.name(),
                account.surname(),
                account.roles(),
                account.blockedUntil(),
                account.blockedReason(),
                account.createdAt(),
                account.updatedAt(),
                stats == null ? null : stats.materialsCount(),
                stats == null ? null : stats.lastMaterialCreatedAt(),
                null,
                null
        );
    }

    @Override
    public java.util.Optional<UserAccount> findById(UserId userId) {
        Objects.requireNonNull(userId, "userId");
        return repository.findById(userId);
    }

    private static void requireAdmin(Actor actor) {
        if (!actor.has(Role.ADMIN)) {
            throw new ForbiddenOperationException("Administrator role required");
        }
    }

    private Set<Role> defaultRolesForNewUser(String email) {
        if (shouldBootstrapAdmin(email)) {
            return Set.of(Role.ADMIN, Role.CREATOR);
        }
        return Set.of(Role.CREATOR);
    }

    private boolean shouldBootstrapAdmin(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        var normalized = normalizeEmail(email);
        if (normalized == null) {
            return false;
        }
        return bootstrapAdmins.adminEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(UserAccountApplicationService::normalizeEmail)
                .anyMatch(normalized::equals);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        var trimmed = email.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static Set<Role> union(Set<Role> left, Set<Role> right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (right.isEmpty()) {
            return left;
        }
        var merged = new java.util.HashSet<Role>(left);
        merged.addAll(right);
        return Set.copyOf(merged);
    }
}
