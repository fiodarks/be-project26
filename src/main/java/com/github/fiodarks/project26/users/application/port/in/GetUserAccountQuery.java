package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.users.domain.model.UserAccount;

import java.util.Optional;

public interface GetUserAccountQuery {
    Optional<UserAccount> findById(UserId userId);
}

