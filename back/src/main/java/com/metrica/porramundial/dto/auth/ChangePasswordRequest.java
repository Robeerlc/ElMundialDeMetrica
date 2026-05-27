package com.metrica.porramundial.dto.auth;

import com.metrica.porramundial.domain.enums.Country;

public record ChangePasswordRequest(String currentPassword, String newPassword, String fullName, Country country) {
}