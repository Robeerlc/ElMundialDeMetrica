package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.User;
import com.metrica.porramundial.dto.AuthResponse;
import com.metrica.porramundial.dto.LoginRequest;
import com.metrica.porramundial.dto.RegisterRequest;
import com.metrica.porramundial.repository.UserRepository;
import com.metrica.porramundial.security.JwtService;
import jakarta.transaction.Transactional;
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
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.email().endsWith("@metrica-global.com"))
            throw new IllegalArgumentException("Solo se permiten correos corporativos (@metrica-global.com).");

        if (userRepository.findByEmail(request.email()).isPresent())
            throw new IllegalArgumentException("El correo ya está registrado.");

        String activationToken = UUID.randomUUID().toString();
        User newUser = User.builder()
                .email(request.email())
                .fullName(request.fullName())
                .password(passwordEncoder.encode(request.password()))
                .country(request.country())
                .department(request.department())
                .isAccountActive(false)
                .activationToken(activationToken).build();
        userRepository.save(newUser);
        emailService.sendActivationEmail(newUser.getEmail(), activationToken);
        return new AuthResponse(null, "Registro exitoso. Revisa tu bandeja de entrada para activar la cuenta.");
    }

    @Transactional
    public String activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de activación inválido o expirado."));
        user.setIsAccountActive(true);
        user.setActivationToken(null);
        userRepository.save(user);
        return "¡Cuenta activada correctamente! Ya puedes iniciar sesión.";
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, "Inicio de sesión exitoso.");
    }
}