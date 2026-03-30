package com.github.fiodarks.project26.archive.adapter.in.web;

import com.github.fiodarks.project26.adapter.in.web.api.AuthenticationApi;
import com.github.fiodarks.project26.adapter.in.web.dto.GoogleAuthorizationUrlResponse;
import com.github.fiodarks.project26.adapter.in.web.dto.GoogleIdTokenRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.LoginRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.RegisterRequest;
import com.github.fiodarks.project26.adapter.in.web.dto.TokenResponse;
import com.github.fiodarks.project26.auth.GoogleOAuthService;
import com.github.fiodarks.project26.auth.LocalAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {

    private final GoogleOAuthService googleOAuthService;
    private final LocalAuthService localAuthService;

    @Override
    public ResponseEntity<GoogleAuthorizationUrlResponse> authGoogleLoginGet() {
        var response = new GoogleAuthorizationUrlResponse();
        response.setAuthorizationUrl(URI.create(googleOAuthService.buildLoginRedirectUri().toString()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> authGoogleLoginRedirectGet() {
        var headers = new HttpHeaders();
        headers.setLocation(googleOAuthService.buildLoginRedirectUri());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Override
    public ResponseEntity<TokenResponse> authGoogleCallbackGet(String code, String state) {
        return ResponseEntity.ok(googleOAuthService.handleCallback(code, state));
    }

    @Override
    public ResponseEntity<TokenResponse> authGoogleTokenPost(GoogleIdTokenRequest googleIdTokenRequest) {
        return ResponseEntity.ok(googleOAuthService.exchangeGoogleIdToken(googleIdTokenRequest));
    }

    @Override
    public ResponseEntity<TokenResponse> authRegisterPost(RegisterRequest registerRequest) {
        return ResponseEntity.ok(localAuthService.register(registerRequest));
    }

    @Override
    public ResponseEntity<TokenResponse> authLoginPost(LoginRequest loginRequest) {
        return ResponseEntity.ok(localAuthService.login(loginRequest));
    }
}
