package com.metrica.porramundial.controller;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.domain.TournamentPhase;
import com.metrica.porramundial.dto.MatchCreateRequest;
import com.metrica.porramundial.dto.MatchResultRequest;
import com.metrica.porramundial.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody MatchCreateRequest request) {
        return ResponseEntity.ok(matchService.createMatch(request));
    }

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/phase/{phase}")
    public ResponseEntity<List<Match>> getMatchesByPhase(@PathVariable TournamentPhase phase) {
        return ResponseEntity.ok(matchService.getMatchesByPhase(phase));
    }

    @PutMapping("/{id}/result")
    public ResponseEntity<Void> updateMatchResult(@PathVariable Long id, @RequestBody MatchResultRequest request) {
        matchService.updateMatchResult(id, request);
        return ResponseEntity.ok().build();
    }
}