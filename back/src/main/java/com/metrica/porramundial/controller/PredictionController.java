package com.metrica.porramundial.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.metrica.porramundial.dto.predictions.PredictionCreationRequest;
import com.metrica.porramundial.dto.predictions.PredictionDataType;
import com.metrica.porramundial.dto.predictions.PredictionResponse;
import com.metrica.porramundial.service.PredictionService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
    public ResponseEntity<PredictionResponse> getPredictionById(@RequestParam Long id) {
        return ResponseEntity.of(this.predictionService.getPredictionById(id));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PredictionResponse>> getPredictionsByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(this.predictionService.getPredictionsByUser(userId));
    }
    
    @PostMapping
    public ResponseEntity<?> createPrediction(@RequestBody PredictionCreationRequest pcr) {
        return PredictionDataType.response(this.predictionService.createPrediction(pcr));
    }
}
