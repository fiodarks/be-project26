package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Actor;

public interface UnblockUserUseCase {
    void unblockUser(Actor actor, UserId userId);
}

