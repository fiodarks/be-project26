package com.github.fiodarks.project26.auth;

import com.github.fiodarks.project26.adapter.in.web.dto.LoginRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.RegisterRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.TokenResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.UserRole;
import com.github.fiodarks.project26.archive.application.exception.ForbiddenOperationException;
import com.github.fiodarks.project26.archive.application.exception.ValidationException;
import com.github.fiodarks.project26.security.Role;
import com.github.fiodarks.project26.users.application.port.in.RegisterLocalUserCommand;
import com.github.fiodarks.project26.users.application.service.UserAccountApplicationService;
import com.github.fiodarks.project26.users.domain.model.UserAccount;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public final class LocalAuthService {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(2);

    private final Clock clock;
    private final JwtEncoder internalJwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountApplicationService users;

    public LocalAuthService(
            Clock clock,
            JwtEncoder internalJwtEncoder,
            PasswordEncoder passwordEncoder,
            UserAccountApplicationService users
    ) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.internalJwtEncoder = Objects.requireNonNull(internalJwtEncoder, "internalJwtEncoder");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");
        this.users = Objects.requireNonNull(users, "users");
    }

    public TokenResponse register(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Request is required");
        }
        String email = requireNonBlank(request.getEmail(), "email");
        String name = requireNonBlank(request.getName(), "name");
        String surname = requireNonBlank(request.getSurname(), "surname");
        String password = requireNonBlank(request.getPassword(), "password");

        String passwordHash = passwordEncoder.encode(password);
        UserAccount created = users.registerLocalUser(new RegisterLocalUserCommand(email, name, surname, passwordHash));
        return issueTokenResponse(created);
    }

    public TokenResponse login(LoginRequest request) {
        if (request == null) {
            throw new ValidationException("Request is required");
        }
        String email = requireNonBlank(request.getEmail(), "email");
        String password = requireNonBlank(request.getPassword(), "password");

        var account = users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (account.passwordHash() == null || account.passwordHash().isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        var now = OffsetDateTime.now(clock);
        if (account.isBlockedAt(now)) {
            throw new ForbiddenOperationException("User is blocked");
        }

        return issueTokenResponse(account);
    }

    private TokenResponse issueTokenResponse(UserAccount account) {
        Objects.requireNonNull(account, "account");
        String token = issueInternalJwt(account.id().value().toString(), account.roles());
        var response = new TokenResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds((int) ACCESS_TOKEN_TTL.toSeconds());
        response.setUserId(account.id().value());
        response.setRoles(account.roles().stream().map(r -> UserRole.valueOf(r.name())).collect(Collectors.toList()));
        return response;
    }

    private String issueInternalJwt(String subject, Set<Role> roles) {
        Instant now = Instant.now(clock);
        var claims = JwtClaimsSet.builder()
                .issuer("project26")
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_TTL))
                .subject(subject)
                .claim("roles", roles.stream().map(Role::name).toList())
                .build();

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return internalJwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}

