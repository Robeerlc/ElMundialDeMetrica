package com.metrica.porramundial.controller;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.match.MatchCreateRequest;
import com.metrica.porramundial.dto.match.MatchResultRequest;
import com.metrica.porramundial.service.MatchService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Match> createMatch(@Valid @RequestBody MatchCreateRequest request) {
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
    public ResponseEntity<Void> updateMatchResult(@PathVariable Long id, @Valid @RequestBody MatchResultRequest request) {
        matchService.updateMatchResult(id, request);
        return ResponseEntity.ok().build();
    }
}