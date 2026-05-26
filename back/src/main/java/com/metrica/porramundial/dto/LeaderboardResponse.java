package com.metrica.porramundial.dto;

public record LeaderboardResponse(Integer rankPosition, String fullName, Integer totalPoints,
                                  Integer exactMatchesCount, Integer goalDiffMatchesCount, Integer winnerMatchesCount) {
}