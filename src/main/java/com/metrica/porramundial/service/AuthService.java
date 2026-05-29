package com.metrica.porramundial.service;

import com.metrica.porramundial.config.security.JwtService;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.dto.auth.RegisterRequest;
import com.metrica.porramundial.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    private static String normalizeEmail(String email) {
        if (email == null)
            throw new IllegalArgumentException("El email no puede ser nulo.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeFullName(String fullName) {
        if (fullName == null)
            throw new IllegalArgumentException("El nombre completo no puede ser nulo.");

        String normalized = fullName.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank())
            throw new IllegalArgumentException("El nombre completo no puede estar vacío.");

        if (!normalized.matches("^[\\p{L} ]+$"))
            throw new IllegalArgumentException("El nombre solo puede contener letras y espacios.");
        return normalized;
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getEmail());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedFullName = normalizeFullName(request.fullName());

        if (userRepository.existsByEmail(normalizedEmail))
            throw new IllegalArgumentException("Este correo ya está registrado.");

        String token = UUID.randomUUID().toString();
        User user = User.builder().email(normalizedEmail).password(passwordEncoder.encode(request.password())).fullName(normalizedFullName).country(request.country()).activationToken(token).enabled(false).build();
        userRepository.save(user);
        enviarCorreoActivacion(user.getEmail(), token);
    }

    @Transactional
    public void activateAccount(String token) {
        if (token == null || token.isBlank()) return;
        userRepository.findByActivationToken(token).ifPresent(user -> {
            if (!user.isEnabled()) user.setEnabled(true);
            user.setActivationToken(null);
            userRepository.save(user);
        });
    }

    private void enviarCorreoActivacion(String emailDestino, String token) {
        try {
            String urlActivacion = "https://porramundialmetrica-championsfinalback-w-59fd54-193-70-44-51.sslip.io/api/auth/activate?token=" + token;
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("noreplay-porra@metrica-global.com");
            helper.setTo(emailDestino);
            helper.setSubject("⚽ ¡Verifica tu cuenta en El Mundial de METRICA!");

            String contenidoHtml = "<!DOCTYPE html>" +
                    "<html lang=\"es\">" +
                    "<head>" +
                    "<meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<style>" +
                    "body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif; background: #0d1117; color: #ffffff; }" +
                    ".container { max-width: 600px; margin: 0 auto; background: #161b22; border: 1px solid #30363d; border-radius: 12px; overflow: hidden; }" +
                    ".header { background: linear-gradient(135deg, #58a6ff, #388bfd); padding: 2rem 1.5rem; text-align: center; }" +
                    ".header h1 { margin: 0; font-size: 1.8rem; font-weight: 900; color: #ffffff; }" +
                    ".content { padding: 2rem 1.5rem; }" +
                    ".greeting { font-size: 16px; color: #c9d1d9; margin-bottom: 1.5rem; line-height: 1.6; }" +
                    ".greeting strong { color: #58a6ff; }" +
                    ".cta-section { background: rgba(88, 166, 255, 0.08); border: 1px solid #30363d; border-radius: 8px; padding: 2rem 1.5rem; text-align: center; margin: 2rem 0; }" +
                    ".cta-text { font-size: 14px; color: #8b949e; margin-bottom: 1.5rem; }" +
                    ".cta-button { display: inline-block; background: #388bfd; color: #ffffff; padding: 0.875rem 2rem; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 15px; transition: opacity 0.2s; }" +
                    ".cta-button:hover { opacity: 0.85; }" +
                    ".footer { padding: 1.5rem; background: #0d1117; border-top: 1px solid #30363d; text-align: center; font-size: 12px; color: #6e7681; }" +
                    ".emoji { font-size: 2rem; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class=\"container\">" +
                    "<div class=\"header\">" +
                    "<h1>⚽ METRICA MUNDIAL 2026</h1>" +
                    "</div>" +
                    "<div class=\"content\">" +
                    "<p class=\"greeting\">¡Hola! 👋</p>" +
                    "<p class=\"greeting\">Gracias por registrarte en la plataforma oficial de <strong>METRICA</strong> para la <strong>Copa Mundial de la FIFA 2026</strong>.</p>" +
                    "<div class=\"cta-section\">" +
                    "<p class=\"cta-text\">Para activar tu cuenta y poder iniciar sesión, haz clic en el botón de abajo:</p>" +
                    "<a href=\"" + urlActivacion + "\" class=\"cta-button\">Activar Cuenta</a>" +
                    "</div>" +
                    "<p class=\"greeting\">Una vez activada tu cuenta, podrás:</p>" +
                    "<p class=\"greeting\" style=\"margin-left: 1rem;\">✓ Hacer predicciones en todos los partidos<br/>✓ Competir en el ranking mundial<br/>✓ Interactuar con otros usuarios en el chat en vivo</p>" +
                    "<p class=\"greeting\" style=\"margin-top: 2rem;\">¡Que disfrutes del torneo! 🏆</p>" +
                    "</div>" +
                    "<div class=\"footer\">" +
                    "<p>Si no te registraste en METRICA, puedes ignorar este correo.</p>" +
                    "<p style=\"margin-top: 0.5rem;\">© 2026 METRICA Global. Todos los derechos reservados.</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            helper.setText(contenidoHtml, true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("Error al enviar correo de activación a " + emailDestino + ": " + e.getMessage());
        }
    }
}