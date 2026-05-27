package com.metrica.porramundial.dto.auth;

import com.metrica.porramundial.domain.enums.Country;

public record RegisterRequest(String email, String password, String fullName, Country country) {
}