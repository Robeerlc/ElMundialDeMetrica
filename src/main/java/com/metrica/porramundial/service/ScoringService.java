package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.entity.Prediction;
import com.metrica.porramundial.domain.entity.PredictionHistory;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.domain.enums.PredictionResultType;
import com.metrica.porramundial.repository.PredictionHistoryRepository;
import com.metrica.porramundial.repository.PredictionRepository;
import com.metrica.porramundial.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public void scoreMatch(Match match) {
        if (match.getHomeGoals() == null || match.getAwayGoals() == null)
            throw new IllegalStateException("Match " + match.getId() + " has no result yet");
        List<Prediction> predictions = predictionRepository.findByMatch(match);

        for (Prediction prediction : predictions) {
            score(prediction, match);
            User user = prediction.getUser();
            user.setTotalPoints(user.getTotalPoints() + prediction.getPointsEarned());
            if (prediction.getResultType() != null) {
                switch (prediction.getResultType()) {
                    case EXACT_MATCH -> user.setExactMatchesCount(user.getExactMatchesCount() + 1);
                    case GOAL_DIFFERENCE -> user.setGoalDiffMatchesCount(user.getGoalDiffMatchesCount() + 1);
                    case WINNER -> user.setWinnerMatchesCount(user.getWinnerMatchesCount() + 1);
                    default -> {
                    }
                }
            }

            userRepository.save(user);
            PredictionHistory history = predictionHistoryRepository
                    .findByUser(user)
                    .orElseGet(() -> {
                        PredictionHistory newHistory = PredictionHistory.builder().user(user).build();
                        return predictionHistoryRepository.save(newHistory);
                    });
            if (!history.getPredictions().contains(prediction)) history.getPredictions().add(prediction);
            history.recalculate();
        }
    }

    private void score(Prediction prediction, Match match) {
        int predHome = prediction.getHomeGoals();
        int predAway = prediction.getAwayGoals();
        int realHome = match.getHomeGoals();
        int realAway = match.getAwayGoals();

        boolean correctWinner = Objects.equals(prediction.getWinningTeam(), match.getWinningTeam());
        boolean correctDiff = (predHome - predAway) == (realHome - realAway);
        boolean exactScore = predHome == realHome && predAway == realAway;

        PredictionResultType resultType;
        if (!correctWinner) resultType = PredictionResultType.LOST;
        else if (exactScore) resultType = PredictionResultType.EXACT_MATCH;
        else if (correctDiff) resultType = PredictionResultType.GOAL_DIFFERENCE;
        else resultType = PredictionResultType.WINNER;

        prediction.setPointsEarned(resultType.getPoints() * match.getPhase().getMultiplier());
        prediction.setResultType(resultType);
    }
}