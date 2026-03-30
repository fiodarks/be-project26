package com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository.spec;

import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.entity.UserAccountJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

public final class UserAccountSpecifications {
    private UserAccountSpecifications() {
    }

    public static Specification<UserAccountJpaEntity> freeText(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        var normalized = q.trim().toLowerCase(Locale.ROOT);
        var like = "%" + normalized + "%";

        return (root, query, cb) -> {
            var email = cb.lower(root.get("email"));
            var name = cb.lower(root.get("name"));
            var surname = cb.lower(root.get("surname"));
            var idAsString = cb.lower(root.get("id").as(String.class));
            return cb.or(
                    cb.like(email, like),
                    cb.like(name, like),
                    cb.like(surname, like),
                    cb.like(idAsString, like)
            );
        };
    }

    public static Specification<UserAccountJpaEntity> hasRole(Role role) {
        if (role == null) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            var join = root.joinSet("roles", JoinType.INNER);
            return cb.equal(join, role);
        };
    }

    public static Specification<UserAccountJpaEntity> blocked(Boolean blocked, OffsetDateTime now) {
        Objects.requireNonNull(now, "now");
        if (blocked == null) {
            return null;
        }
        return (root, query, cb) -> {
            var blockedUntil = root.get("blockedUntil").as(OffsetDateTime.class);
            if (blocked) {
                return cb.greaterThan(blockedUntil, now);
            }
            return cb.or(
                cb.isNull(blockedUntil),
                cb.lessThanOrEqualTo(blockedUntil, now)
            );
        };
    }
}
