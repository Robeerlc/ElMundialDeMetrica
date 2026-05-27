package com.metrica.porramundial.dto.auth;

import com.metrica.porramundial.domain.enums.Country;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El email no puede estar vacío")
        @Email(message = "El email debe ser válido")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password,

        @NotBlank(message = "El nombre completo no puede estar vacío")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String fullName,

        @NotNull(message = "El país no puede ser nulo")
        Country country
) {
}