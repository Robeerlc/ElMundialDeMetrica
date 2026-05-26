package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.match.MatchCreateRequest;
import com.metrica.porramundial.dto.match.MatchResultRequest;
import com.metrica.porramundial.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Transactional
    public Match createMatch(MatchCreateRequest request) {
        Match match = Match.builder()
                .homeTeam(request.homeTeam())
                .awayTeam(request.awayTeam())
                .startTime(request.startTime())
                .phase(request.phase())
                .status(MatchStatus.PENDING)
                .isLocked(false)
                .build();

        return matchRepository.save(match);
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public List<Match> getMatchesByPhase(TournamentPhase phase) {
        return matchRepository.findByPhase(phase);
    }

    @Transactional
    public void updateMatchResult(Long matchId, MatchResultRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Partido no encontrado con ID: " + matchId));

        if (match.getStatus() == MatchStatus.FINISHED)
            throw new IllegalArgumentException("El partido ya está finalizado y no se puede modificar.");

        match.setHomeGoals(request.homeGoals());
        match.setAwayGoals(request.awayGoals());
        match.setWinningTeam(request.winningTeam());
        match.setStatus(MatchStatus.FINISHED);
        match.setEndTime(LocalDateTime.now());
        match.setIsLocked(true);
        matchRepository.save(match);
    }
}