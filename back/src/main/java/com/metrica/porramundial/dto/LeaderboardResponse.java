package com.metrica.porramundial.dto;

import com.metrica.porramundial.domain.enums.Country;

public record LeaderboardResponse(Integer rankPosition, String fullName, Integer totalPoints,
                                  Integer exactMatchesCount, Integer goalDiffMatchesCount, Integer winnerMatchesCount, Country country) {
}