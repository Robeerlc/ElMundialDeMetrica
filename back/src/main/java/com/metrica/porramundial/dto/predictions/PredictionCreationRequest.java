package com.metrica.porramundial.dto.predictions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PredictionCreationRequest(@NotNull(message = "El ID del partido no puede ser nulo") Long idMatch,

                                        @NotNull(message = "Los goles del equipo local no pueden ser nulos") @Min(value = 0, message = "Los goles no pueden ser negativos") Integer homeGoals,

                                        @NotNull(message = "Los goles del equipo visitante no pueden ser nulos") @Min(value = 0, message = "Los goles no pueden ser negativos") Integer awayGoals,

                                        @NotNull(message = "El campo isDraw no puede ser nulo") Boolean isDraw,

                                        String winningTeam) {
}