package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.security.Actor;

import java.time.OffsetDateTime;

public interface BlockUserUseCase {
    void blockUser(Actor actor, UserId userId, OffsetDateTime blockedUntil, String reason);
}

