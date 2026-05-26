package com.metrica.porramundial.dto.predictions;

import org.springframework.http.ResponseEntity;

public sealed interface PredictionDataType
        permits PredictionDataType.Fail,
        PredictionDataType.Created {
    static ResponseEntity<?> response(PredictionDataType pct) {
        return switch (pct) {
            case PredictionDataType.Fail(String reason) -> ResponseEntity.badRequest().body(reason);
            case PredictionDataType.Created c -> ResponseEntity.status(201).build();
        };
    }

    record Fail(String reason) implements PredictionDataType {
    }

    record Created() implements PredictionDataType {
    }
}