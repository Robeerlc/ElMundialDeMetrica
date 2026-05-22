package com.metrica.porramundial.repository;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.domain.MatchStatus;
import com.metrica.porramundial.domain.TournamentPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByPhase(TournamentPhase phase);
    List<Match> findByStatus(MatchStatus status);
    List<Match> findByHomeTeamOrAwayTeam(String homeTeam, String awayTeam);
}