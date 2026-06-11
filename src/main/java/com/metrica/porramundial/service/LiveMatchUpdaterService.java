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
        String[] competitions = {"WC"};

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
                            try {
                                System.out.println("[LIVE] matchId=" + liveFixture.id() + " status=" + apiStatus + " score=" + liveFixture.score() + " penalties=" + (liveFixture.score() != null ? liveFixture.score().penalties() : null));
                            } catch (Exception ignore) {
                            }

                            int newHomeGoals = resolveHomeGoals(liveFixture);
                            int newAwayGoals = resolveAwayGoals(liveFixture);

                            int finalHomeGoals = newHomeGoals != -1 ? newHomeGoals : (dbMatch.getHomeGoals() != null ? dbMatch.getHomeGoals() : 0);
                            int finalAwayGoals = newAwayGoals != -1 ? newAwayGoals : (dbMatch.getAwayGoals() != null ? dbMatch.getAwayGoals() : 0);

                            if ("IN_PLAY".equals(apiStatus) || "PAUSED".equals(apiStatus)) {
                                if (dbMatch.getStatus() == MatchStatus.PENDING) {
                                    dbMatch.setStatus(MatchStatus.IN_PROGRESS);
                                    dbMatch.setIsLocked(true);
                                    System.out.println("¡PARTIDO EN JUEGO! Bloqueando predicciones para: " + dbMatch.getHomeTeam() + " vs " + dbMatch.getAwayTeam());
                                }
                                dbMatch.setHomeGoals(finalHomeGoals);
                                dbMatch.setAwayGoals(finalAwayGoals);
                                System.out.println("[UPDATE] " + dbMatch.getHomeTeam() + "-" + dbMatch.getAwayTeam() + " -> " + finalHomeGoals + "-" + finalAwayGoals + " (penalties ignored)");
                                matchRepository.save(dbMatch);

                            } else if ("FINISHED".equals(apiStatus) || "AWARDED".equals(apiStatus)) {
                                if (dbMatch.getStatus() != MatchStatus.FINISHED) {
                                    String winnerField = null;
                                    if (liveFixture.score() != null) winnerField = liveFixture.score().winner();

                                    String winningTeam = null;
                                    if ("HOME_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getHomeTeam();
                                    else if ("AWAY_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getAwayTeam();
                                    else {
                                        if (finalHomeGoals > finalAwayGoals) winningTeam = dbMatch.getHomeTeam();
                                        else if (finalAwayGoals > finalHomeGoals) winningTeam = dbMatch.getAwayTeam();
                                    }

                                    dbMatch.setStatus(MatchStatus.FINISHED);
                                    dbMatch.setIsLocked(true);
                                    dbMatch.setHomeGoals(finalHomeGoals);
                                    dbMatch.setAwayGoals(finalAwayGoals);
                                    dbMatch.setWinningTeam(winningTeam);

                                    System.out.println("[FINISHED] " + dbMatch.getHomeTeam() + "-" + dbMatch.getAwayTeam() + " final " + finalHomeGoals + "-" + finalAwayGoals + " (Winner=" + winningTeam + ")");
                                    matchRepository.save(dbMatch);
                                    System.out.println("PARTIDO TERMINADO (" + finalHomeGoals + "-" + finalAwayGoals + "). Calculando puntos de los usuarios...");
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
        if (liveFixture.score() == null) return -1;
        FootballDataResponse.ScoreData score = liveFixture.score();

        if (score.regularTime() != null && score.regularTime().home() != null) {
            int goals = score.regularTime().home();
            if (score.extraTime() != null && score.extraTime().home() != null) {
                goals += score.extraTime().home();
            }
            return goals;
        }

        if (score.fullTime() != null && score.fullTime().home() != null) {
            int goals = score.fullTime().home();
            if (score.penalties() != null && score.penalties().home() != null) goals -= score.penalties().home();
            return Math.max(0, goals);
        }
        return -1;
    }

    private int resolveAwayGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return -1;
        FootballDataResponse.ScoreData score = liveFixture.score();

        if (score.regularTime() != null && score.regularTime().away() != null) {
            int goals = score.regularTime().away();
            if (score.extraTime() != null && score.extraTime().away() != null) goals += score.extraTime().away();
            return goals;
        }

        if (score.fullTime() != null && score.fullTime().away() != null) {
            int goals = score.fullTime().away();
            if (score.penalties() != null && score.penalties().away() != null) goals -= score.penalties().away();
            return Math.max(0, goals);
        }
        return -1;
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