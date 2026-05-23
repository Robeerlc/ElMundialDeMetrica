package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.*;
import com.metrica.porramundial.repository.PredictionHistoryRepository;
import com.metrica.porramundial.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final PredictionRepository predictionRepository;
    private final PredictionHistoryRepository predictionHistoryRepository;

    @Transactional
    public void scoreMatch(Match match) {
        if (match.getHomeGoals() == null || match.getAwayGoals() == null) {
            throw new IllegalStateException("Match " + match.getId() + " has no result yet");
        }

        List<Prediction> predictions = predictionRepository.findByMatch(match);

        for (Prediction prediction : predictions) {
            score(prediction, match);
            predictionRepository.save(prediction);

            PredictionHistory history = predictionHistoryRepository
                    .findByUser(prediction.getUser())
                    .orElseGet(() -> PredictionHistory.builder()
                            .user(prediction.getUser())
                            .build());

            if (!history.getPredictions().contains(prediction)) {
                history.getPredictions().add(prediction);
            }

            history.recalculate();
            predictionHistoryRepository.save(history);
        }
    }

    private void score(Prediction prediction, Match match) {
        int predHome = prediction.getHomeGoals();
        int predAway = prediction.getAwayGoals();
        int realHome = match.getHomeGoals();
        int realAway = match.getAwayGoals();

        boolean correctWinner = Objects.equals(prediction.getWinningTeam(), match.getWinningTeam());
        boolean correctDiff   = (predHome - predAway) == (realHome - realAway);
        boolean exactScore    = predHome == realHome && predAway == realAway;

        PredictionResultType resultType;
        if (!correctWinner) {
            resultType = PredictionResultType.LOST;
        } else if (exactScore) {
            resultType = PredictionResultType.EXACT_MATCH;
        } else if (correctDiff) {
            resultType = PredictionResultType.GOAL_DIFFERENCE;
        } else {
            resultType = PredictionResultType.WINNER;
        }

        prediction.setPointsEarned(resultType.getPoints() * match.getPhase().getMultiplier());
        prediction.setResultType(resultType);
    }
}
