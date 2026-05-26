package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.auth.AuthResponse;
import com.metrica.porramundial.dto.auth.ChangePasswordRequest;
import com.metrica.porramundial.dto.auth.LoginRequest;
import com.metrica.porramundial.repository.UserRepository;
import com.metrica.porramundial.config.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email()).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getEmail(), user.getRequirePasswordChange());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword()))
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);
    }
}