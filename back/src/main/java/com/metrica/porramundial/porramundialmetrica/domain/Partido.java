package com.metrica.porramundial.porramundialmetrica.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String equipoLocal;

    @Column(nullable = false)
    private String equipoVisitante;

    private Integer golesLocal;

    private Integer golesVisitante;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFinal;

    @Enumerated(EnumType.STRING)
    private EstadoPartido estadoPartido;

    @Enumerated(EnumType.STRING)
    private FasePartido fasePartido;

    private String equipoGanador;

    @Column(nullable = false)
    private Boolean bloqueado;
}
