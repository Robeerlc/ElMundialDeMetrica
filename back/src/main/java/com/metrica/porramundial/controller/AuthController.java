package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.dto.auth.RegisterRequest;
import com.metrica.porramundial.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activate(@RequestParam String token) {
        authService.activateAccount(token);
        String htmlResponse = "<html><body><h2>¡Cuenta activada con éxito!</h2><p>Ya puedes volver a la aplicacion e iniciar sesion.</p></body></html>";
        return ResponseEntity.ok().header("Content-Type", "text/html").body(htmlResponse);
    }
}