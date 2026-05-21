package com.metrica.porramundial.porramundialmetrica.domain;

public enum FasePartido {
    GRUPOS(1),
    OCTAVOS(2),
    CUARTOS(4),
    SEMIFINAL(6),
    TERCER_Y_CUARTO(8),
    FINAL(10);

    private final int multiplicador;

    FasePartido(int multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int getMultiplicador() { return this.multiplicador; }
}
