package com.metrica.porramundial.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.domain.Prediction;
import com.metrica.porramundial.domain.User;

public interface PredictionRepository extends JpaRepository<Prediction, Long>{
    java.util.List<Prediction> findByUser(User user);
    java.util.List<Prediction> findByMatch(Match match);
    java.util.Optional<Prediction> findByUserAndMatch(User user, Match match);
}
