package com.metrica.porramundial.dto;

import java.util.List;

public record FootballDataResponse(List<MatchData> matches) {

    public record MatchData(Long id, String utcDate, String status, String stage, TeamData homeTeam, TeamData awayTeam,
                            ScoreData score) {
    }

    public record TeamData(String shortName) {
    }

    public record ScoreData(TimeData fullTime) {
    }

    public record TimeData(Integer home, Integer away) {
    }
}