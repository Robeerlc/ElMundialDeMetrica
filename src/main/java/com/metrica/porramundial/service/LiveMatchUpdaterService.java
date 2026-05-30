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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<Long, Match> activeMatchesMap = activeMatches.stream()
                .collect(Collectors.toMap(Match::getApiMatchId, m -> m));
        List<Match> matchesToSave = new ArrayList<>();
        String[] competitions = {"WC"};

        for (String comp : competitions) {
            try {
                FootballDataResponse apiResponse = restClient.get()
                        .uri("/competitions/" + comp + "/matches")
                        .retrieve()
                        .body(FootballDataResponse.class);
                if (apiResponse == null || apiResponse.matches() == null) continue;
                for (FootballDataResponse.MatchData liveFixture : apiResponse.matches()) {
                    Match dbMatch = activeMatchesMap.get(liveFixture.id());
                    if (dbMatch != null) {
                        String apiStatus = liveFixture.status();
                        int homeGoals = resolveHomeGoals(liveFixture);
                        int awayGoals = resolveAwayGoals(liveFixture);
                        boolean updated = false;
                        if ("IN_PLAY".equals(apiStatus) || "PAUSED".equals(apiStatus)) {
                            if (dbMatch.getStatus() == MatchStatus.PENDING) {
                                dbMatch.setStatus(MatchStatus.IN_PROGRESS);
                                dbMatch.setIsLocked(true);
                                System.out.println("¡PARTIDO EN JUEGO! Bloqueando predicciones para: " + dbMatch.getHomeTeam() + " vs " + dbMatch.getAwayTeam());
                            }
                            dbMatch.setHomeGoals(homeGoals);
                            dbMatch.setAwayGoals(awayGoals);
                            updated = true;
                        } else if ("FINISHED".equals(apiStatus) || "AWARDED".equals(apiStatus)) {
                            if (dbMatch.getStatus() != MatchStatus.FINISHED) {
                                String winnerField = null;
                                if (liveFixture.score() != null) winnerField = liveFixture.score().winner();
                                String winningTeam = null;
                                if ("HOME_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getHomeTeam();
                                else if ("AWAY_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getAwayTeam();
                                else {
                                    if (homeGoals > awayGoals) winningTeam = dbMatch.getHomeTeam();
                                    else if (awayGoals > homeGoals) winningTeam = dbMatch.getAwayTeam();
                                }
                                dbMatch.setStatus(MatchStatus.FINISHED);
                                dbMatch.setIsLocked(true);
                                dbMatch.setHomeGoals(homeGoals);
                                dbMatch.setAwayGoals(awayGoals);
                                dbMatch.setWinningTeam(winningTeam);
                                updated = true;

                                System.out.println("[FINISHED] " + dbMatch.getHomeTeam() + "-" + dbMatch.getAwayTeam() + " final " + homeGoals + "-" + awayGoals + " (Winner=" + winningTeam + ")");
                                this.scoringService.scoreMatch(dbMatch);
                            }
                        }
                        if (updated) matchesToSave.add(dbMatch);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al contactar con Football-Data para " + comp + ": " + e.getMessage());
            }
        }
        if (!matchesToSave.isEmpty()) matchRepository.saveAll(matchesToSave);
    }

    private int resolveHomeGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return 0;
        if (liveFixture.score().fullTime() != null && liveFixture.score().fullTime().home() != null)
            return liveFixture.score().fullTime().home();
        if (liveFixture.score().regularTime() != null && liveFixture.score().regularTime().home() != null)
            return liveFixture.score().regularTime().home();
        return 0;
    }

    private int resolveAwayGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return 0;
        if (liveFixture.score().fullTime() != null && liveFixture.score().fullTime().away() != null)
            return liveFixture.score().fullTime().away();
        if (liveFixture.score().regularTime() != null && liveFixture.score().regularTime().away() != null)
            return liveFixture.score().regularTime().away();
        return 0;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void lockUpcomingMatches() {
        LocalDateTime horaActualUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        List<Match> activeMatches = matchRepository.findByStatusNot(MatchStatus.FINISHED);
        List<Match> matchesToUpdate = new ArrayList<>();
        for (Match m : activeMatches) {
            if (m.getStatus() == MatchStatus.PENDING) {
                boolean shouldBeLocked = horaActualUtc.isAfter(m.getStartTime().minusHours(1));
                if (m.getIsLocked() != shouldBeLocked) {
                    m.setIsLocked(shouldBeLocked);
                    matchesToUpdate.add(m);

                    if (shouldBeLocked) {
                        System.out.println("[BLOQUEO] Tiempo límite superado. Partido cerrado: " + m.getHomeTeam() + " vs " + m.getAwayTeam());
                    } else {
                        System.out.println("[DESBLOQUEO AUTO-CORRECCIÓN] Partido reabierto: " + m.getHomeTeam() + " vs " + m.getAwayTeam());
                    }
                }
            }
        }
        if (!matchesToUpdate.isEmpty()) matchRepository.saveAll(matchesToUpdate);
    }
}