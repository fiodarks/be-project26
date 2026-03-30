package com.github.fiodarks.project26.users.application.port.in;

import com.github.fiodarks.project26.commons.PageResult;

public record UsersAdminPage(PageResult<UserAdminSummary> page) {
}

