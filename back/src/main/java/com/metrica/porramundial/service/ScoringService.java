package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.*;
import com.metrica.porramundial.repository.PredictionHistoryRepository;
import com.metrica.porramundial.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        int predDiff = predHome - predAway;
        int realDiff = realHome - realAway;

        PredictionResultType resultType;
        if (predHome == realHome && predAway == realAway) {
            resultType = PredictionResultType.EXACT_MATCH;
        } else if (predDiff == realDiff) {
            resultType = PredictionResultType.GOAL_DIFFERENCE;
        } else if (Integer.signum(predDiff) == Integer.signum(realDiff)) {
            resultType = PredictionResultType.WINNER;
        } else {
            resultType = PredictionResultType.LOST;
        }

        prediction.setPointsEarned(resultType.getPoints() * match.getPhase().getMultiplier());
        prediction.setResultType(resultType);
    }
}
