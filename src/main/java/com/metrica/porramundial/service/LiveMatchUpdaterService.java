package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

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

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void lockUpcomingPhases() {
        System.out.println("Revisando si hay fases que deban bloquearse...");
        for (TournamentPhase phase : TournamentPhase.values()) {
            Optional<Match> firstMatchOp = matchRepository.findFirstByPhaseOrderByStartTimeAsc(phase);
            if (firstMatchOp.isEmpty()) continue;

            Match firstMatch = firstMatchOp.get();
            java.time.LocalDateTime phaseLockTime = firstMatch.getStartTime().minusHours(24);

            if (java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).isAfter(phaseLockTime)) {
                List<Match> matchesToLock = matchRepository.findByPhase(phase).stream()
                        .filter(m -> !m.getIsLocked())
                        .toList();

                if (!matchesToLock.isEmpty()) {
                    matchesToLock.forEach(m -> m.setIsLocked(true));
                    matchRepository.saveAll(matchesToLock);
                    System.out.println("Bloqueados " + matchesToLock.size() + " partidos de la fase " + phase);
                }
            }
        }
    }
}