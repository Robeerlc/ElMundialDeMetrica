package com.metrica.porramundial.dto.predictions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;

public record PredictionCreationRequest(@NotNull(message = "El ID del partido no puede ser nulo") Long idMatch,

                                        @NotNull(message = "Los goles del equipo local no pueden ser nulos") @Min(value = 0, message = "Los goles no pueden ser negativos") @Max(value = 30, message = "Los goles no pueden ser mayores a 30") @Digits(integer = 2, fraction = 0, message = "Los goles deben ser números enteros") Integer homeGoals,

                                        @NotNull(message = "Los goles del equipo visitante no pueden ser nulos") @Min(value = 0, message = "Los goles no pueden ser negativos") @Max(value = 30, message = "Los goles no pueden ser mayores a 30") @Digits(integer = 2, fraction = 0, message = "Los goles deben ser números enteros") Integer awayGoals,

                                        @NotNull(message = "El campo isDraw no puede ser nulo") Boolean isDraw,

                                        String winningTeam) {
}