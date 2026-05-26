package com.metrica.porramundial.dto;

public record LeaderboardResponse(Integer rankPosition, String fullName, String department, Integer totalPoints,
                                  Integer exactMatchesCount, Integer goalDiffMatchesCount, Integer winnerMatchesCount) {
}