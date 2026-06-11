package com.metrica.porramundial.controller;

import com.metrica.porramundial.service.ScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ScoringService scoringService;

    public AdminController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/match/{matchId}/reset")
    public ResponseEntity<String> resetMatch(@PathVariable Long matchId) {
        try {
            scoringService.resetMatchAndRecalculate(matchId);
            return ResponseEntity.ok("Partido " + matchId + " reseteado y ranking recalculado con éxito.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al resetear el partido: " + e.getMessage());
        }
    }
}