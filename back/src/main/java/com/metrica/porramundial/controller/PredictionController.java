package com.metrica.porramundial.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metrica.porramundial.dto.predictions.PredictionCreationRequest;
import com.metrica.porramundial.dto.predictions.PredictionDataType;
import com.metrica.porramundial.dto.predictions.PredictionResponse;
import com.metrica.porramundial.service.PredictionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;
    
    @GetMapping
    public ResponseEntity<List<PredictionResponse>> getAllPredictions() {
        return ResponseEntity.ok(this.predictionService.getAllPredictions());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PredictionResponse> getPredictionById(@PathVariable Long id) {
        return ResponseEntity.of(this.predictionService.getPredictionById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<PredictionResponse>> getMyPredictions(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(this.predictionService.getPredictionsByUsername(username));
    }
    
    @PostMapping
    public ResponseEntity<?> createPrediction(@RequestBody PredictionCreationRequest pcr, Authentication authentication) {
        return PredictionDataType.response(this.predictionService.createPrediction(pcr, authentication));
    }
}