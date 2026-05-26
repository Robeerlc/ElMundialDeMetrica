package com.metrica.porramundial.dto.predictions;

public record PredictionCreationRequest(
        Long idMatch,
        Integer homeGoals,
        Integer awayGoals,
        Boolean isDraw,
        String winningTeam
) {
}