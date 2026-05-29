package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.predictions.PredictionCreationRequest;
import com.metrica.porramundial.dto.predictions.PredictionDataType;
import com.metrica.porramundial.dto.predictions.PredictionResponse;
import com.metrica.porramundial.service.PredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;

    @GetMapping
    public ResponseEntity<List<PredictionResponse>> getAllPredictions() {
        return ResponseEntity.ok(this.predictionService.getAllPredictions());
    }

    @GetMapping("/me")
    public ResponseEntity<List<PredictionResponse>> getMyPredictions(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(this.predictionService.getPredictionsByUsername(username));
    }

    @PostMapping
    public ResponseEntity<?> createPrediction(@Valid @RequestBody PredictionCreationRequest pcr, Authentication authentication) {
        return PredictionDataType.response(this.predictionService.createPrediction(pcr, authentication));
    }
}