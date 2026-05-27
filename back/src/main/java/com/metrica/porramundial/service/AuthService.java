package com.metrica.porramundial.service;

import com.metrica.porramundial.config.security.JwtService;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.dto.auth.RegisterRequest;
import com.metrica.porramundial.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getEmail());
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("Este correo ya está registrado.");

        String token = UUID.randomUUID().toString();
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .country(request.country())
                .activationToken(token)
                .enabled(false)
                .build();
        userRepository.save(user);
        enviarCorreoActivacion(user.getEmail(), token);
    }

    @Transactional
    public void activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de activación no es válido o ya ha sido usado."));
        user.setEnabled(true);
        user.setActivationToken(null);
        userRepository.save(user);
    }

    private void enviarCorreoActivacion(String emailDestino, String token) {
        try {
            String urlActivacion = "http://localhost:8080/api/auth/activate?token=" + token;
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("noreplay-porra@metrica-global.com");
            mensaje.setTo(emailDestino);
            mensaje.setSubject("⚽ ¡Verifica tu cuenta en El Mundial de METRICA!");
            mensaje.setText("¡Hola!\n\n" +
                    "Gracias por registrarte en la plataforma oficial de la porra de METRICA.\n\n" +
                    "Para activar tu cuenta y poder iniciar sesión, solo tienes que hacer clic en el siguiente enlace:\n" +
                    "👉 " + urlActivacion + "\n\n" +
                    "¡Mucha suerte en tus pronósticos! 🏆");
            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("Error al enviar correo de activación a " + emailDestino + ": " + e.getMessage());
        }
    }
}