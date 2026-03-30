package com.github.fiodarks.project26.users;

import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.entity.MaterialJpaEntity;
import com.github.fiodarks.project26.archive.adapter.out.persistence.jpa.repository.MaterialSpringDataRepository;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.entity.UserAccountJpaEntity;
import com.github.fiodarks.project26.users.adapter.out.persistence.jpa.repository.UserAccountSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsersAdminListIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAccountSpringDataRepository usersRepository;

    @Autowired
    MaterialSpringDataRepository materialsRepository;

    @Test
    void unauthenticated_list_users_is_401() throws Exception {
        mvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void non_admin_list_users_is_403() throws Exception {
        mvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("VIEWER")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_list_and_filter_users() throws Exception {
        materialsRepository.deleteAll();
        usersRepository.deleteAll();

        var now = OffsetDateTime.now();

        var adminId = UUID.randomUUID();
        persistUser(adminId, "admin@example.com", "Admin", Set.of(Role.ADMIN), null, null, now.minusSeconds(3));

        var blockedUserId = UUID.randomUUID();
        persistUser(
                blockedUserId,
                "blocked@example.com",
                "Blocked Creator",
                Set.of(Role.CREATOR),
                now.plusDays(1),
                "Spam uploads",
                now.minusSeconds(2)
        );

        var viewerId = UUID.randomUUID();
        persistUser(viewerId, "viewer@example.com", "Viewer", Set.of(Role.VIEWER), null, null, now.minusSeconds(1));

        persistMaterial(blockedUserId, now.minusDays(2));
        persistMaterial(blockedUserId, now.minusDays(1));

        mvc.perform(get("/api/v1/users")
                        .with(jwt().jwt(j -> j.subject(adminId.toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalElements").value(3))
                .andExpect(jsonPath("$.items[?(@.userId == '%s')].materialsCount".formatted(blockedUserId)).value(contains(2)))
                .andExpect(jsonPath("$.items[?(@.userId == '%s')].blockedReason".formatted(blockedUserId)).value(hasItem("Spam uploads")));

        mvc.perform(get("/api/v1/users")
                        .param("blocked", "true")
                        .with(jwt().jwt(j -> j.subject(adminId.toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(blockedUserId.toString()));

        mvc.perform(get("/api/v1/users")
                        .param("role", "VIEWER")
                        .with(jwt().jwt(j -> j.subject(adminId.toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(viewerId.toString()));

        mvc.perform(get("/api/v1/users")
                        .param("q", "blocked@")
                        .with(jwt().jwt(j -> j.subject(adminId.toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(blockedUserId.toString()));
    }

    @Test
    void invalid_size_is_400() throws Exception {
        mvc.perform(get("/api/v1/users")
                        .param("size", "501")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()).claim("roles", List.of("ADMIN")))))
                .andExpect(status().isBadRequest());
    }

    private void persistUser(
            UUID userId,
            String email,
            String name,
            Set<Role> roles,
            OffsetDateTime blockedUntil,
            String blockedReason,
            OffsetDateTime createdAt
    ) {
        var entity = new UserAccountJpaEntity();
        entity.setId(userId);
        entity.setEmail(email);
        entity.setName(name);
        entity.setPictureUrl(null);
        entity.setRoles(new java.util.HashSet<>(roles));
        entity.setBlockedUntil(blockedUntil);
        entity.setBlockedReason(blockedReason);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        usersRepository.save(entity);
    }

    private void persistMaterial(UUID ownerId, OffsetDateTime createdAt) {
        var material = new MaterialJpaEntity();
        material.setId(UUID.randomUUID());
        material.setVersion(0L);
        material.setTitle("T");
        material.setLocation("Warsaw");
        material.setCreationDateRaw("2000");
        material.setCreationDateFrom(LocalDate.of(2000, 1, 1));
        material.setCreationDateTo(LocalDate.of(2000, 12, 31));
        material.setDescription("D");
        material.setHierarchyId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        material.setOwnerId(ownerId);
        material.setFileUrl(null);
        material.setThumbnailUrl(null);
        material.setCreatedAt(createdAt);
        material.setUpdatedAt(createdAt);
        materialsRepository.save(material);
    }
}
