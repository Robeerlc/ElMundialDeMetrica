package com.metrica.porramundial.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    private Integer homeGoals;
    private Integer awayGoals;

    @Builder.Default
    private Integer pointsEarned = 0;

    private Boolean isDraw;
    private String winningTeam;

    @Enumerated(EnumType.STRING)
    private PredictionResultType resultType;
}