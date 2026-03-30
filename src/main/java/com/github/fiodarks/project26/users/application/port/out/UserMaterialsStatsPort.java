package com.github.fiodarks.project26.users.application.port.out;

import com.github.fiodarks.project26.archive.domain.model.UserId;

import java.util.Map;
import java.util.Set;

public interface UserMaterialsStatsPort {
    Map<UserId, UserMaterialStats> findStatsByUserIds(Set<UserId> userIds);
}

