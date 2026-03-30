package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Actor;

public interface GetUserAdminDetailsQuery {
    UserAdminDetails getUserAdminDetails(Actor actor, UserId userId);
}

