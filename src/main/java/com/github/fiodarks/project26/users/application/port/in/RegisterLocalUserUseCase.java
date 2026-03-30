package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.users.domain.model.UserAccount;

public interface RegisterLocalUserUseCase {
    UserAccount registerLocalUser(RegisterLocalUserCommand command);
}

