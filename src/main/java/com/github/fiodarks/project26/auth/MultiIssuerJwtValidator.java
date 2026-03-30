package com.github.fiodarks.project26.auth;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.List;
import java.util.Objects;

public final class MultiIssuerJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final List<OAuth2TokenValidator<Jwt>> delegates;

    public MultiIssuerJwtValidator(List<OAuth2TokenValidator<Jwt>> delegates) {
        if (delegates == null || delegates.isEmpty() || delegates.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("delegates must not be null/empty");
        }
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        OAuth2TokenValidatorResult lastFailure = null;
        for (var delegate : delegates) {
            var result = delegate.validate(token);
            if (result != null && result.hasErrors()) {
                lastFailure = result;
                continue;
            }
            return OAuth2TokenValidatorResult.success();
        }
        return lastFailure == null
                ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token issuer (iss) is not accepted", null))
                : lastFailure;
    }
}
