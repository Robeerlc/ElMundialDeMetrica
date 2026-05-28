package com.metrica.porramundial.domain.enums;

import lombok.Getter;

@Getter
public enum TournamentPhase {
    GROUP_STAGE(1),
    ROUND_OF_16(2),
    QUARTER_FINALS(4),
    SEMI_FINALS(6),
    THIRD_PLACE(8),
    FINAL(10);

    private final int multiplier;

    TournamentPhase(int multiplier) {
        this.multiplier = multiplier;
    }
}