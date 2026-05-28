package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.entity.Prediction;
import com.metrica.porramundial.domain.entity.PredictionHistory;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.domain.enums.TournamentPhase;
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

        Match firstMatchOfPhase = matchRepository.findFirstByPhaseOrderByStartTimeAsc(match.getPhase())
                .orElseThrow(() -> new IllegalStateException("No se encontraron partidos para la fase: " + match.getPhase()));
        java.time.LocalDateTime phaseLockTime = firstMatchOfPhase.getStartTime().minusHours(24);

        if (java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).isAfter(phaseLockTime)) {
            return new PredictionDataType.Fail("¡Demasiado tarde! Las apuestas para la fase de " + match.getPhase() + " se cerraron 24 horas antes de su primer partido.");
        }

        if (match.getIsLocked()) {
            return new PredictionDataType.Fail("Match is locked!");
        }

        if (this.predictionRepository.findByUserAndMatch(user, match).isPresent()) {
            return new PredictionDataType.Fail("Already made a prediction");
        }

        boolean isDraw = pcr.homeGoals().equals(pcr.awayGoals());
        if (isDraw && match.getPhase() != TournamentPhase.GROUP_STAGE) {
            if (pcr.winningTeam() == null || pcr.winningTeam().isBlank()) {
                return new PredictionDataType.Fail("En eliminatorias, si predices un empate, debes elegir quién ganará por penaltis.");
            }
            if (!pcr.winningTeam().equalsIgnoreCase(match.getHomeTeam()) &&
                    !pcr.winningTeam().equalsIgnoreCase(match.getAwayTeam())) {
                return new PredictionDataType.Fail("El ganador debe ser obligatoriamente " + match.getHomeTeam() + " o " + match.getAwayTeam());
            }
        }

        Prediction prediction = Prediction.builder()
                .user(user)
                .match(match)
                .awayGoals(pcr.awayGoals())
                .homeGoals(pcr.homeGoals())
                .isDraw(isDraw)
                .winningTeam(pcr.winningTeam())
                .build();

        if (!isDraw) {
            prediction.setWinningTeam(
                    pcr.homeGoals() > pcr.awayGoals()
                            ? match.getHomeTeam()
                            : match.getAwayTeam()
            );
        } else if (match.getPhase() == TournamentPhase.GROUP_STAGE) {
            prediction.setWinningTeam(null);
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