package com.metrica.porramundial.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.domain.Prediction;
import com.metrica.porramundial.domain.User;
import com.metrica.porramundial.dto.predictions.PredictionCreationRequest;
import com.metrica.porramundial.dto.predictions.PredictionDataType;
import com.metrica.porramundial.dto.predictions.PredictionResponse;
import com.metrica.porramundial.repository.MatchRepository;
import com.metrica.porramundial.repository.PredictionRepository;
import com.metrica.porramundial.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PredictionService {
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
        
        if (prediction.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(PredictionResponse.of(prediction.get()));
    }

    public List<PredictionResponse> getPredictionsByUser(Long userId) {
        Optional<User> user = this.userRepository.findById(userId);
        if (user.isEmpty()) {
            return List.of();
        }
        return this.predictionRepository.findByUser(user.get())
                .stream()
                .map(PredictionResponse::of)
                .toList();
    }
    
    public PredictionDataType createPrediction(PredictionCreationRequest pcr) {
        Optional<User> userOp = this.userRepository.findById(pcr.idUser());
        if (userOp.isEmpty()) { return new PredictionDataType.Fail("User not found"); }
        
        Optional<Match> matchOp = matchRepository.findById(pcr.idMatch());
        if (matchOp.isEmpty()) { return new PredictionDataType.Fail("Match not found"); }
        
        User user = userOp.get();
        Match match = matchOp.get();
        
        Prediction prediction = Prediction.builder()
                                          .user(user)
                                          .match(match)
                                          .awayGoals(pcr.awayGoals())
                                          .homeGoals(pcr.homeGoals())
                                          .isDraw(pcr.homeGoals().equals(pcr.awayGoals()))
                                          .isDraw(false)
                                          .winningTeam(pcr.winningTeam())
                                          .build();
        
        if (! pcr.isDraw()) {
            prediction.setWinningTeam(
                pcr.homeGoals() > pcr.awayGoals()
                                ? match.getHomeTeam()
                                : match.getAwayTeam()
            );
        }
        
        this.predictionRepository.save(prediction);
        
        return new PredictionDataType.Created();
    }
}
