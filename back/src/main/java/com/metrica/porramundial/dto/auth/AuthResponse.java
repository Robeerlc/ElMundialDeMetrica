package com.metrica.porramundial.dto.auth;

public record AuthResponse(String token, String email, Boolean requirePasswordChange) {
}