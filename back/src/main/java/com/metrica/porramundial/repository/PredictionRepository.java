package com.metrica.porramundial.repository;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.entity.Prediction;
import com.metrica.porramundial.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    java.util.List<Prediction> findByUser(User user);

    java.util.List<Prediction> findByMatch(Match match);

    java.util.Optional<Prediction> findByUserAndMatch(User user, Match match);
}
