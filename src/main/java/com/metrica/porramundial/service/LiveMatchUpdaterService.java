package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LiveMatchUpdaterService {

    private final MatchRepository matchRepository;
    private final RestClient restClient;
    private final ScoringService scoringService;

    public LiveMatchUpdaterService(
            MatchRepository matchRepository,
            @Value("${api.footballdata.url}") String apiUrl,
            @Value("${api.footballdata.key}") String apiKey,
            ScoringService scoringService
    ) {
        this.matchRepository = matchRepository;
        this.restClient = RestClient.builder().baseUrl(apiUrl).defaultHeader("X-Auth-Token", apiKey).build();
        this.scoringService = scoringService;
    }

    @Scheduled(fixedRate = 60000)
    public void fetchLiveResults() {
        List<Match> activeMatches = matchRepository.findByStatusNot(MatchStatus.FINISHED);
        if (activeMatches.isEmpty()) return;
        String[] competitions = {"WC", "CL"};

        for (String comp : competitions) {
            try {
                FootballDataResponse apiResponse = restClient.get()
                        .uri("/competitions/" + comp + "/matches")
                        .retrieve()
                        .body(FootballDataResponse.class);

                if (apiResponse == null || apiResponse.matches() == null) continue;
                for (FootballDataResponse.MatchData liveFixture : apiResponse.matches()) {
                    for (Match dbMatch : activeMatches) {
                        if (dbMatch.getApiMatchId().equals(liveFixture.id())) {

                            String apiStatus = liveFixture.status();
                            // debug: print score and penalties to inspect if penalties are being reported
                            try {
                                System.out.println("[LIVE] matchId=" + liveFixture.id() + " status=" + apiStatus + " score=" + liveFixture.score() + " penalties=" + (liveFixture.score() != null ? liveFixture.score().penalties() : null));
                            } catch (Exception ignore) {
                            }

                            int homeGoals = resolveHomeGoals(liveFixture);
                            int awayGoals = resolveAwayGoals(liveFixture);
                            if ("IN_PLAY".equals(apiStatus) || "PAUSED".equals(apiStatus)) {
                                if (dbMatch.getStatus() == MatchStatus.PENDING) {
                                    dbMatch.setStatus(MatchStatus.IN_PROGRESS);
                                    dbMatch.setIsLocked(true);
                                    System.out.println("¡PARTIDO EN JUEGO! Bloqueando predicciones para: " + dbMatch.getHomeTeam() + " vs " + dbMatch.getAwayTeam());
                                }
                                // update goals (we explicitly IGNORE penalty-shootout goals which are in score.penalties())
                                dbMatch.setHomeGoals(homeGoals);
                                dbMatch.setAwayGoals(awayGoals);
                                System.out.println("[UPDATE] " + dbMatch.getHomeTeam() + "-" + dbMatch.getAwayTeam() + " -> " + homeGoals + "-" + awayGoals + " (penalties ignored)");
                                matchRepository.save(dbMatch);
                            } else if ("FINISHED".equals(apiStatus) || "AWARDED".equals(apiStatus)) {
                                if (dbMatch.getStatus() != MatchStatus.FINISHED) {
                                    String winningTeam = null;
                                    if (homeGoals > awayGoals) winningTeam = dbMatch.getHomeTeam();
                                    else if (awayGoals > homeGoals) winningTeam = dbMatch.getAwayTeam();
                                    dbMatch.setStatus(MatchStatus.FINISHED);
                                    dbMatch.setIsLocked(true);
                                    // when match finishes, use the resolved goals (penalties are ignored here)
                                    dbMatch.setHomeGoals(homeGoals);
                                    dbMatch.setAwayGoals(awayGoals);
                                    System.out.println("[FINISHED] " + dbMatch.getHomeTeam() + "-" + dbMatch.getAwayTeam() + " final " + homeGoals + "-" + awayGoals + " (penalties ignored: " + (liveFixture.score() != null ? liveFixture.score().penalties() : null) + ")");
                                    dbMatch.setWinningTeam(winningTeam);
                                    matchRepository.save(dbMatch);
                                    System.out.println("PARTIDO TERMINADO (" + homeGoals + "-" + awayGoals + "). Calculando puntos de los usuarios...");
                                    this.scoringService.scoreMatch(dbMatch);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al contactar con Football-Data para " + comp + ": " + e.getMessage());
            }
        }
    }

    private int resolveHomeGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return 0;
        FootballDataResponse.ScoreData score = liveFixture.score();
        String status = liveFixture.status();
        // For live matches prefer current-running values (regularTime, halfTime, extraTime), avoid using penalties
        if ("IN_PLAY".equals(status) || "PAUSED".equals(status)) {
            if (score.regularTime() != null && score.regularTime().home() != null) return score.regularTime().home();
            if (score.halfTime() != null && score.halfTime().home() != null) return score.halfTime().home();
            if (score.extraTime() != null && score.extraTime().home() != null) return score.extraTime().home();
            if (score.fullTime() != null && score.fullTime().home() != null) return score.fullTime().home();
            return 0;
        } else {
            // For finished/awarded matches prefer fullTime then extraTime, but still ignore penalties
            if (score.fullTime() != null && score.fullTime().home() != null) return score.fullTime().home();
            if (score.extraTime() != null && score.extraTime().home() != null) return score.extraTime().home();
            if (score.regularTime() != null && score.regularTime().home() != null) return score.regularTime().home();
            if (score.halfTime() != null && score.halfTime().home() != null) return score.halfTime().home();
            return 0;
        }
    }

    private int resolveAwayGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return 0;
        FootballDataResponse.ScoreData score = liveFixture.score();
        String status = liveFixture.status();
        // For live matches prefer current-running values (regularTime, halfTime, extraTime), avoid using penalties
        if ("IN_PLAY".equals(status) || "PAUSED".equals(status)) {
            if (score.regularTime() != null && score.regularTime().away() != null) return score.regularTime().away();
            if (score.halfTime() != null && score.halfTime().away() != null) return score.halfTime().away();
            if (score.extraTime() != null && score.extraTime().away() != null) return score.extraTime().away();
            if (score.fullTime() != null && score.fullTime().away() != null) return score.fullTime().away();
            return 0;
        } else {
            // For finished/awarded matches prefer fullTime then extraTime, but still ignore penalties
            if (score.fullTime() != null && score.fullTime().away() != null) return score.fullTime().away();
            if (score.extraTime() != null && score.extraTime().away() != null) return score.extraTime().away();
            if (score.regularTime() != null && score.regularTime().away() != null) return score.regularTime().away();
            if (score.halfTime() != null && score.halfTime().away() != null) return score.halfTime().away();
            return 0;
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void lockUpcomingMatches() {
        LocalDateTime horaActualUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        List<Match> activeMatches = matchRepository.findByStatusNot(MatchStatus.FINISHED);
        for (Match m : activeMatches) {
            if (m.getStatus() == MatchStatus.PENDING) {
                boolean shouldBeLocked = horaActualUtc.isAfter(m.getStartTime().minusHours(1));
                if (m.getIsLocked() != shouldBeLocked) {
                    m.setIsLocked(shouldBeLocked);
                    matchRepository.save(m);

                    if (shouldBeLocked)
                        System.out.println("[BLOQUEO] Tiempo límite superado. Partido cerrado: " + m.getHomeTeam() + " vs " + m.getAwayTeam());
                    else
                        System.out.println("[DESBLOQUEO AUTO-CORRECCIÓN] Partido reabierto: " + m.getHomeTeam() + " vs " + m.getAwayTeam());

                }
            }
        }
    }
}