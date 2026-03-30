package com.github.fiodarks.project26.users.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.api.UsersApi;
import com.github.fiodarks.project26.adapter.in.web.dto.CurrentUserResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.UserRole;
import com.github.fiodarks.project26.archive.adapter.in.web.security.SpringSecurityActorResolver;
import com.github.fiodarks.project26.users.application.service.UserAccountApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UsersController implements UsersApi {

    private final SpringSecurityActorResolver actorResolver;
    private final UserAccountApplicationService users;

    @Override
    public ResponseEntity<CurrentUserResponse> usersMeGet() {
        var actor = actorResolver.requireActor();
        var account = users.getOrCreateCurrentUser(actor);

        var response = new CurrentUserResponse();
        response.setUserId(account.id().value());
        response.setRoles(account.roles().stream()
                .map(r -> UserRole.valueOf(r.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        response.setBlockedUntil(account.blockedUntil());
        response.setBlockedReason(account.blockedReason());
        return ResponseEntity.ok(response);
    }
}
