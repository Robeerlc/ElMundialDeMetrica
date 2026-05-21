package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.AuthResponse;
import com.metrica.porramundial.dto.LoginRequest;
import com.metrica.porramundial.dto.RegisterRequest;
import com.metrica.porramundial.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam String token) {
        return ResponseEntity.ok(authService.activateAccount(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}