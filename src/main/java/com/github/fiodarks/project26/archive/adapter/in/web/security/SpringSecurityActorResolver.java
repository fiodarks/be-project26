package com.github.fiodarks.project26.archive.adapter.in.web.security;

import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.archive.domain.model.UserId;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpringSecurityActorResolver {

    private static final Actor ANONYMOUS_VIEWER = new Actor(new UserId(new UUID(0L, 0L)), Set.of(Role.VIEWER));

    public Actor requireActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        var userId = extractUserId(authentication);
        var roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(SpringSecurityActorResolver::toRole)
                .filter(r -> r != null)
                .collect(Collectors.toUnmodifiableSet());

        if (roles.isEmpty()) {
            roles = Set.of(Role.VIEWER);
        }

        return new Actor(new UserId(userId), roles);
    }

    public Actor resolveActorOrAnonymous() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ANONYMOUS_VIEWER;
        }
        return requireActor();
    }

    private static UUID extractUserId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwt) {
            var subject = jwt.getToken().getSubject();
            if (subject != null) {
                return UUID.fromString(subject);
            }
        }
        var principal = authentication.getPrincipal();
        if (principal instanceof String s) {
            return UUID.fromString(s);
        }
        throw new org.springframework.security.access.AccessDeniedException("User id not available");
    }

    private static Role toRole(String authority) {
        if (authority == null) {
            return null;
        }
        var normalized = authority.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return switch (normalized) {
            case "ADMIN" -> Role.ADMIN;
            case "CREATOR" -> Role.CREATOR;
            case "VIEWER" -> Role.VIEWER;
            default -> null;
        };
    }
}
