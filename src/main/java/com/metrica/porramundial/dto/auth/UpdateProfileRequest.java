package com.metrica.porramundial.dto.auth;

public record UpdateProfileRequest(String avatar, String currentPassword, String newPassword) {
}