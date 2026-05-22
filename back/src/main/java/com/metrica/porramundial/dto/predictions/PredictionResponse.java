package com.metrica.porramundial.dto.predictions;

import com.metrica.porramundial.domain.Prediction;

public record PredictionResponse(
    Long id,
    Long userId,
    Long matchId,
    Integer homeGoals,
    Integer awayGoals,
    Integer pointsEarned,
    Boolean isDraw,
    String winningTeam
) {
    public static PredictionResponse of(Prediction prediction) {
        return new PredictionResponse(
            prediction.getId(),
            prediction.getUser().getId(),
            prediction.getMatch().getId(),
            prediction.getHomeGoals(),
            prediction.getAwayGoals(),
            prediction.getPointsEarned(),
            prediction.getIsDraw(),
            prediction.getWinningTeam()
        );
    }
}
