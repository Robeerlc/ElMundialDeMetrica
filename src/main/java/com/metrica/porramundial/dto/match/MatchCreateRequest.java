package com.metrica.porramundial.dto.match;

import com.metrica.porramundial.domain.enums.TournamentPhase;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record MatchCreateRequest(
        @NotBlank(message = "El equipo local no puede estar vacío")
        @Size(min = 2, max = 100, message = "El nombre del equipo debe tener entre 2 y 100 caracteres")
        String homeTeam,

        @NotBlank(message = "El equipo visitante no puede estar vacío")
        @Size(min = 2, max = 100, message = "El nombre del equipo debe tener entre 2 y 100 caracteres")
        String awayTeam,

        @NotNull(message = "La hora de inicio no puede ser nula")
        @Future(message = "La hora de inicio debe ser en el futuro")
        LocalDateTime startTime,

        @NotNull(message = "La fase del torneo no puede ser nula")
        TournamentPhase phase
) {
}