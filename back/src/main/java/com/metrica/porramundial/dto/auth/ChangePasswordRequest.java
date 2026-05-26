package com.metrica.porramundial.dto.auth;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}