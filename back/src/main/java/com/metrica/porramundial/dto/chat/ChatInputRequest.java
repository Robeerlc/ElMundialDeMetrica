package com.metrica.porramundial.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatInputRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(min = 1, max = 2000, message = "El mensaje debe tener entre 1 y 2000 caracteres")
        String message
) {
}