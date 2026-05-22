package com.metrica.porramundial.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String homeTeam;

    @Column(nullable = false)
    private String awayTeam;

    private Integer homeGoals;
    private Integer awayGoals;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Enumerated(EnumType.STRING)
    private TournamentPhase phase;

    private String winningTeam;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isLocked = false;
}