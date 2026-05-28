package com.metrica.porramundial.repository;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByPhase(TournamentPhase phase);

    Optional<Match> findFirstByPhaseOrderByStartTimeAsc(TournamentPhase phase);

    boolean existsByApiMatchId(Long apiMatchId);

    List<Match> findByStatusNot(MatchStatus status);
}