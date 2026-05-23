package com.metrica.porramundial.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.metrica.porramundial.domain.PredictionHistory;
import com.metrica.porramundial.domain.User;

public interface PredictionHistoryRepository
         extends JpaRepository<PredictionHistory, Long> {
    java.util.Optional<PredictionHistory> findByUser(User user);
}
