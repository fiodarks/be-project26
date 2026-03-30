package com.github.fiodarks.project26.users.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.UserId;
import com.github.fiodarks.project26.commons.PageResult;
import com.github.fiodarks.project26.users.domain.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepositoryPort {
    Optional<UserAccount> findById(UserId id);

    Optional<UserAccount> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);

    UserAccount save(UserAccount account);

    boolean existsAnyAdmin();

    PageResult<UserAccount> search(UserAccountSearchCriteria criteria);
}
