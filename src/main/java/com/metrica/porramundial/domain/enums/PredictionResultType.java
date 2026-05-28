package com.metrica.porramundial.domain.enums;

import lombok.Getter;

@Getter
public enum PredictionResultType {
    LOST(0),
    WINNER(1),
    GOAL_DIFFERENCE(2),
    EXACT_MATCH(3);

    private final int points;

    PredictionResultType(int points) {
        this.points = points;
    }
}