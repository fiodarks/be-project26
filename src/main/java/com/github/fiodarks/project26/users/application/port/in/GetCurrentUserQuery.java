package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.security.Actor;
import com.github.fiodarks.project26.users.domain.model.UserAccount;

public interface GetCurrentUserQuery {
    UserAccount getOrCreateCurrentUser(Actor actor);
}

