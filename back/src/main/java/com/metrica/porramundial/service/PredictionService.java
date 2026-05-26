package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.entity.Prediction;
import com.metrica.porramundial.domain.entity.PredictionHistory;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.predictions.PredictionCreationRequest;
import com.metrica.porramundial.dto.predictions.PredictionDataType;
import com.metrica.porramundial.dto.predictions.PredictionResponse;
import com.metrica.porramundial.repository.MatchRepository;
import com.metrica.porramundial.repository.PredictionHistoryRepository;
import com.metrica.porramundial.repository.PredictionRepository;
import com.metrica.porramundial.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PredictionService {
    private final PredictionHistoryRepository predictionHistoryRepository;
    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    public List<PredictionResponse> getAllPredictions() {
        return this.predictionRepository.findAll()
                .stream()
                .map(PredictionResponse::of)
                .toList();
    }

    public Optional<PredictionResponse> getPredictionById(Long id) {
        Optional<Prediction> prediction = this.predictionRepository.findById(id);
        return prediction.map(PredictionResponse::of);
    }

    public List<PredictionResponse> getPredictionsByUsername(String username) {
        Optional<User> user = this.userRepository.findByEmail(username);
        return user.map(value -> this.predictionRepository.findByUser(value)
                .stream()
                .map(PredictionResponse::of)
                .toList()).orElseGet(List::of);
    }

    @Transactional
    public PredictionDataType createPrediction(PredictionCreationRequest pcr, Authentication authentication) {
        String username = authentication.getName();

        Optional<User> userOp = this.userRepository.findByEmail(username);
        if (userOp.isEmpty()) {
            return new PredictionDataType.Fail("User not found");
        }

        Optional<Match> matchOp = matchRepository.findById(pcr.idMatch());
        if (matchOp.isEmpty()) {
            return new PredictionDataType.Fail("Match not found");
        }

        User user = userOp.get();
        Match match = matchOp.get();

        if (match.getIsLocked()) {
            return new PredictionDataType.Fail("Match is locked!");
        }

        if (this.predictionRepository.findByUserAndMatch(user, match).isPresent()) {
            return new PredictionDataType.Fail("Already made a prediction");
        }

        Prediction prediction = Prediction.builder()
                .user(user)
                .match(match)
                .awayGoals(pcr.awayGoals())
                .homeGoals(pcr.homeGoals())
                .isDraw(pcr.homeGoals().equals(pcr.awayGoals()))
                .winningTeam(pcr.winningTeam())
                .build();

        if (!pcr.isDraw()) {
            prediction.setWinningTeam(
                    pcr.homeGoals() > pcr.awayGoals()
                            ? match.getHomeTeam()
                            : match.getAwayTeam()
            );
        }

        this.predictionRepository.save(prediction);

        PredictionHistory history = this.predictionHistoryRepository
                .findByUser(user)
                .orElseGet(() -> PredictionHistory.builder()
                        .user(user)
                        .build()
                );

        history.getPredictions().add(prediction);
        this.predictionHistoryRepository.save(history);

        return new PredictionDataType.Created();
    }
}
