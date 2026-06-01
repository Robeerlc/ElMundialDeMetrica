package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.auth.PasswordUpdateRequest;
import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.dto.auth.RegisterRequest;
import com.metrica.porramundial.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activate(@RequestParam String token) {
        authService.activateAccount(token);
        String htmlResponse = """
                <!DOCTYPE html>
                                <html lang="es">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <title>Cuenta Activada</title>
                                    <style>
                                        * { margin: 0; padding: 0; box-sizing: border-box; }
                                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif; background: #0d1117; color: #ffffff; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
                                        .container { background: #161b22; border: 1px solid #30363d; border-radius: 12px; max-width: 500px; width: 100%; overflow: hidden; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6); }
                                        .header { background: linear-gradient(135deg, #58a6ff, #388bfd); padding: 2.5rem 2rem; text-align: center; }
                                        .header h1 { font-size: 1.8rem; font-weight: 900; color: #ffffff; margin: 0; }
                                        .content { padding: 2.5rem 2rem; text-align: center; }
                                        .emoji { font-size: 3.5rem; margin-bottom: 1.5rem; display: block; animation: bounce 0.6s ease-in-out; }
                                        @keyframes bounce { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
                                        h2 { font-size: 1.6rem; font-weight: 900; color: #58a6ff; margin-bottom: 1rem; }
                                        p { font-size: 15px; color: #c9d1d9; line-height: 1.6; margin-bottom: 1rem; }
                                        .highlight { color: #58a6ff; font-weight: 600; }
                                        .info-box { background: rgba(88, 166, 255, 0.08); border: 1px solid rgba(88, 166, 255, 0.25); border-radius: 8px; padding: 1.5rem; margin: 1.5rem 0; }
                                        .info-box p { font-size: 14px; color: #8b949e; margin: 0; }
                                        .footer { background: #0d1117; border-top: 1px solid #30363d; padding: 1.5rem; text-align: center; font-size: 12px; color: #6e7681; }
                                        .cta-link { display: inline-block; background: #388bfd; color: #ffffff; padding: 0.75rem 1.75rem; border-radius: 6px; text-decoration: none; font-weight: 600; margin-top: 1.5rem; transition: opacity 0.2s; }
                                        .cta-link:hover { opacity: 0.85; }
                                    </style>
                                </head>
                                <body>
                                    <div class="container">
                                        <div class="header">
                                            <h1>⚽ METRICA MUNDIAL 2026</h1>
                                        </div>
                                        <div class="content">
                
                                            <h2>¡Cuenta Activada!</h2>
                                            <div class="info-box">
                                                <p>Si el enlace ya había sido usado o tu cuenta estaba activada previamente, no hay problema: tu cuenta sigue siendo válida.</p>
                                            </div>
                                            <p>Ya puedes iniciar sesión y comenzar a hacer tus predicciones en la Copa Mundial de la FIFA 2026.</p>
                                        </div>
                                    </div>
                                </body>
                                </html>
                """;
        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(htmlResponse);
    }

    @PatchMapping("/password-change")
    public ResponseEntity<?> changePassword(@RequestBody PasswordUpdateRequest request, Authentication authentication) {
        String username = authentication.getName();
        this.authService.changePassword(request, username);
        return ResponseEntity.ok().build();
    }
}