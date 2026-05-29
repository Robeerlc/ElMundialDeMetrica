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
                            int homeGoals = (liveFixture.score() != null && liveFixture.score().fullTime() != null && liveFixture.score().fullTime().home() != null) ? liveFixture.score().fullTime().home() : 0;
                            int awayGoals = (liveFixture.score() != null && liveFixture.score().fullTime() != null && liveFixture.score().fullTime().away() != null) ? liveFixture.score().fullTime().away() : 0;
                            if ("IN_PLAY".equals(apiStatus) || "PAUSED".equals(apiStatus)) {
                                if (dbMatch.getStatus() == MatchStatus.PENDING) {
                                    dbMatch.setStatus(MatchStatus.IN_PROGRESS);
                                    dbMatch.setIsLocked(true);
                                    System.out.println("¡PARTIDO EN JUEGO! Bloqueando predicciones para: " + dbMatch.getHomeTeam() + " vs " + dbMatch.getAwayTeam());
                                }
                                dbMatch.setHomeGoals(homeGoals);
                                dbMatch.setAwayGoals(awayGoals);
                                matchRepository.save(dbMatch);
                            } else if ("FINISHED".equals(apiStatus) || "AWARDED".equals(apiStatus)) {
                                if (dbMatch.getStatus() != MatchStatus.FINISHED) {
                                    String winningTeam = null;
                                    if (homeGoals > awayGoals) winningTeam = dbMatch.getHomeTeam();
                                    else if (awayGoals > homeGoals) winningTeam = dbMatch.getAwayTeam();
                                    dbMatch.setStatus(MatchStatus.FINISHED);
                                    dbMatch.setIsLocked(true);
                                    dbMatch.setHomeGoals(homeGoals);
                                    dbMatch.setAwayGoals(awayGoals);
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