package com.metrica.porramundial.dto.predictions;

import com.metrica.porramundial.domain.entity.Prediction;

public record PredictionResponse(
        Long id,
        String username,
        Long matchId,
        Boolean isLocked,
        Integer homeGoals,
        Integer awayGoals,
        Integer pointsEarned,
        Boolean isDraw,
        String winningTeam
) {
    public static PredictionResponse of(Prediction prediction) {
        return new PredictionResponse(
                prediction.getId(),
                prediction.getUser().getUsername(),
                prediction.getMatch().getId(),
                prediction.getMatch().getIsLocked(),
                prediction.getHomeGoals(),
                prediction.getAwayGoals(),
                prediction.getPointsEarned(),
                prediction.getIsDraw(),
                prediction.getWinningTeam()
        );
    }
}