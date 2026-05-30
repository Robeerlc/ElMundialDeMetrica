package com.metrica.porramundial.dto;

import java.util.List;

public record FootballDataResponse(List<MatchData> matches) {

    public record MatchData(Long id, String utcDate, String status, String stage, TeamData homeTeam, TeamData awayTeam,
                            ScoreData score) {
    }

    public record TeamData(String shortName) {
    }

    // Added extraTime and penalties fields to map all possible score sub-objects from football-data
    public record ScoreData(TimeData fullTime, TimeData regularTime, TimeData halfTime, TimeData extraTime, TimeData penalties) {
    }

    public record TimeData(Integer home, Integer away) {
    }
}