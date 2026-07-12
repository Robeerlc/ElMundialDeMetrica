package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.*;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.PredictionResultType;
import com.metrica.porramundial.repository.*;
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
    private final MatchRepository matchRepository;
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
                    default -> {}
                }
            }

            PredictionHistory history = predictionHistoryRepository.findByUser(user)
                    .orElseGet(() -> predictionHistoryRepository.save(PredictionHistory.builder().user(user).build()));
            if (!history.getPredictions().contains(prediction)) history.getPredictions().add(prediction);
            history.recalculate();
        }
    }

    private void score(Prediction prediction, Match match) {
        int predHome = prediction.getHomeGoals();
        int predAway = prediction.getAwayGoals();
        int realHome = match.getHomeGoals();
        int realAway = match.getAwayGoals();

        String predWinner = prediction.getWinningTeam() != null ? prediction.getWinningTeam().trim() : "";
        String realWinner = match.getWinningTeam() != null ? match.getWinningTeam().trim() : "";
        boolean correctWinner = predWinner.equalsIgnoreCase(realWinner);
        
        boolean exactScore = predHome == realHome && predAway == realAway;
        boolean correctDiff = (predHome - predAway) == (realHome - realAway);

        PredictionResultType resultType;

        if (!correctWinner) resultType = PredictionResultType.LOST;
        else if (exactScore) resultType = PredictionResultType.EXACT_MATCH;
        else if (correctDiff) resultType = PredictionResultType.GOAL_DIFFERENCE;
        else resultType = PredictionResultType.WINNER;
        
        prediction.setPointsEarned(resultType.getPoints() * match.getPhase().getMultiplier());
        prediction.setResultType(resultType);
    }

    @Transactional
    public void resetMatchAndRecalculate(Long matchId, Integer homeGoals, Integer awayGoals) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setWinningTeam(null);
        if (homeGoals != null) match.setHomeGoals(homeGoals);
        if (awayGoals != null) match.setAwayGoals(awayGoals);
        matchRepository.save(match);

        List<Prediction> predictions = predictionRepository.findByMatch(match);
        for (Prediction p : predictions) {
            p.setPointsEarned(0);
            p.setResultType(null);
        }
        predictionRepository.saveAll(predictions);

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            int totalPoints = 0;
            int exactMatches = 0;
            int goalDiff = 0;
            int winnerMatches = 0;

            List<Prediction> userPredictions = predictionRepository.findByUser(user);
            for (Prediction p : userPredictions) {
                if (p.getPointsEarned() != null && p.getPointsEarned() > 0) {
                    totalPoints += p.getPointsEarned();
                    if (p.getResultType() != null) {
                        switch (p.getResultType()) {
                            case EXACT_MATCH -> exactMatches++;
                            case GOAL_DIFFERENCE -> goalDiff++;
                            case WINNER -> winnerMatches++;
                            default -> {}
                        }
                    }
                }
            }
            user.setTotalPoints(totalPoints);
            user.setExactMatchesCount(exactMatches);
            user.setGoalDiffMatchesCount(goalDiff);
            user.setWinnerMatchesCount(winnerMatches);
        }
        userRepository.saveAll(allUsers);
        System.out.println("[ADMIN] Ranking recalculado tras el reseteo.");
    }
}
