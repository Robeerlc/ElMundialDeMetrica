package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.ChangePasswordRequest;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        authService.changePassword(request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}