package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.dto.auth.RegisterRequest;
import com.metrica.porramundial.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                        body { font-family: Arial, sans-serif; text-align: center; padding: 40px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
                        .container { background: white; padding: 30px; border-radius: 10px; max-width: 600px; margin: 0 auto; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                        h2 { color: #28a745; margin: 0 0 20px 0; }
                        p { color: #333; line-height: 1.6; }
                        .emoji { font-size: 2.5em; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>¡Cuenta Activada con Éxito!</h2>
                        <div class="emoji">✅</div>
                        <p>Tu cuenta para las predicciones del Mundial de METRICA está lista para usar.</p>
                        <p>Si el enlace ya había sido usado o tu cuenta estaba activada, no hay problema: puedes iniciar sesión directamente.</p>
                        <p style="margin-top: 30px; font-size: 0.9em; color: #666;">¡Que disfrutes del torneo! ⚽ 🏆</p>
                    </div>
                </body>
                </html>
                """;
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(htmlResponse);
    }
}