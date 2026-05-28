package com.metrica.porramundial.controller;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/ongoing")
    public ResponseEntity<List<Match>> getAllMatchesOngoing() {
        return ResponseEntity.ok(matchService.getAllMatchesOngoing());
    }

    @GetMapping("/phase/{phase}")
    public ResponseEntity<List<Match>> getMatchesByPhase(@PathVariable TournamentPhase phase) {
        return ResponseEntity.ok(matchService.getMatchesByPhase(phase));
    }
}