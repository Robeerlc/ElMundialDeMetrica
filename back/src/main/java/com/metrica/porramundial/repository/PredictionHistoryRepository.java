package com.metrica.porramundial.repository;

import com.metrica.porramundial.domain.entity.PredictionHistory;
import com.metrica.porramundial.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredictionHistoryRepository
        extends JpaRepository<PredictionHistory, Long> {
    java.util.Optional<PredictionHistory> findByUser(User user);
}