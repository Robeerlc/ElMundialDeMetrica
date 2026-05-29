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
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token no puede estar vacío.");
        }

        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de activación no es válido."));

        if (!user.isEnabled()) user.setEnabled(true);
        user.setActivationToken(null);
        userRepository.save(user);
    }

    private void enviarCorreoActivacion(String emailDestino, String token) {
        try {
            String urlActivacion = "https://porramundialmetrica-championsfinalback-w-59fd54-193-70-44-51.sslip.io/api/auth/activate?token=" + token;
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom("noreplay-porra@metrica-global.com");
            helper.setTo(emailDestino);
            helper.setSubject("⚽ ¡Verifica tu cuenta en El Mundial de METRICA!");

            String contenidoHtml = "<p>¡Hola!</p>" + "<p>Gracias por registrarte en la plataforma oficial de METRICA para la <strong>Copa Mundial de la FIFA 2026</strong>.</p>" + "<p>Para activar tu cuenta y poder iniciar sesión, solo tienes que hacer clic en el siguiente enlace:</p>" + "<p>👉 <a href=\"" + urlActivacion + "\" style=\"color: #0056b3; font-weight: bold; text-decoration: none;\">¡Activa tu cuenta y participa ya!</a></p>" + "<p>¡Mucha suerte en tus pronósticos! 🏆</p>";
            helper.setText(contenidoHtml, true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("Error al enviar correo de activación a " + emailDestino + ": " + e.getMessage());
        }
    }
}