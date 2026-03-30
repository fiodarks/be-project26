package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.security.Role;

import java.util.Set;

public interface SetUserRolesUseCase {
    void setUserRoles(Actor actor, UserId userId, Set<Role> roles);
}

