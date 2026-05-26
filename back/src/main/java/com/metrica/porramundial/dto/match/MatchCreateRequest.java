package com.metrica.porramundial.dto.match;

import com.metrica.porramundial.domain.enums.TournamentPhase;

import java.time.LocalDateTime;

public record MatchCreateRequest(String homeTeam, String awayTeam, LocalDateTime startTime, TournamentPhase phase) {
}