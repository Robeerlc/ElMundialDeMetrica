package com.metrica.porramundial.dto.auth;

public record PasswordUpdateRequest(
    String oldPassword,
    String newPassword
) {}
