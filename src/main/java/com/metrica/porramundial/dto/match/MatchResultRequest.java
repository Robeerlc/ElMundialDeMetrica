package com.metrica.porramundial.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchResultRequest(
        @NotNull(message = "Los goles del equipo local no pueden ser nulos")
        @Min(value = 0, message = "Los goles no pueden ser negativos")
        Integer homeGoals,

        @NotNull(message = "Los goles del equipo visitante no pueden ser nulos")
        @Min(value = 0, message = "Los goles no pueden ser negativos")
        Integer awayGoals,

        String winningTeam
) {
}