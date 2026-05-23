package com.metrica.porramundial.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "prediction_history_id")
    @Builder.Default
    private List<Prediction> predictions = new ArrayList<>();

    @Builder.Default
    private Integer totalPoints = 0;

    public void addPrediction(Prediction prediction) {
        predictions.add(prediction);
        totalPoints += prediction.getPointsEarned();
    }
    
    public void recalculate() {
        this.totalPoints = predictions.stream()
                .mapToInt(p -> p.getPointsEarned() != null ? p.getPointsEarned() : 0)
                .sum();
    }
}