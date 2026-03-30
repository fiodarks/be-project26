package com.github.fiodarks.project26.archive.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.time.OffsetDateTime;

public interface UserBlockStatusPort {
    boolean isBlocked(UserId userId, OffsetDateTime at);
}

