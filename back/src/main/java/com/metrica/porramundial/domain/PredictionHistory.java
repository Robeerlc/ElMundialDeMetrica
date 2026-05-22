package com.metrica.porramundial.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prediction_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id")
    private Prediction prediction;

    @Column(nullable = false)
    private Integer pointsEarned;
}