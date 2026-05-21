package com.metrica.porramundial.porramundialmetrica.domain;

public enum PronosticoResuelto {
    PERDIDO(0),
    GANADOR(1),
    DIFERENCIA(2),
    EXACTO(3);
    
    private final int puntos;
    
    private PronosticoResuelto(int puntos) {
        this.puntos = puntos;
    }
    
    public int getPuntos() { return this.puntos; }
}
