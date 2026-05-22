package com.metrica.porramundial.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.metrica.porramundial.domain.Match;

public interface MatchRepository extends JpaRepository<Match, Long>{

}
