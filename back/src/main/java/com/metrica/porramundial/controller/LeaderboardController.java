package com.metrica.porramundial.controller;

import com.metrica.porramundial.dto.LeaderboardResponse;
import com.metrica.porramundial.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardResponse>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getLeaderboard());
    }
    
    @GetMapping("/me")
    public ResponseEntity<LeaderboardResponse> getMyLeaderboard(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.of(leaderboardService.getMyLeaderboard(email));
    }
}