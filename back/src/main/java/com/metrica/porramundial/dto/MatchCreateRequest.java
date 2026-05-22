package com.metrica.porramundial.dto;

import com.metrica.porramundial.domain.TournamentPhase;

import java.time.LocalDateTime;

public record MatchCreateRequest(String homeTeam, String awayTeam, LocalDateTime startTime, TournamentPhase phase) {}