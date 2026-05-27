package com.metrica.porramundial.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El email no puede estar vacío") @Email(message = "El email debe ser válido") String email,

        @NotBlank(message = "La contraseña no puede estar vacía") @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres") String password) {
}