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
    @Transactional
    public void fetchLiveResults() {
        List<Match> activeMatches = matchRepository.findByStatusNot(MatchStatus.FINISHED);
        if (activeMatches.isEmpty()) return;

        Map<String, Match> activeMatchesMap = activeMatches.stream()
                .collect(Collectors.toMap(m -> String.valueOf(m.getApiMatchId()), match -> match));

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
                    Match dbMatch = activeMatchesMap.get(String.valueOf(liveFixture.id()));
                    if (dbMatch != null) {
                        String apiStatus = liveFixture.status();
                        int newHomeGoals = resolveHomeGoals(liveFixture);
                        int newAwayGoals = resolveAwayGoals(liveFixture);

                        int finalHomeGoals = newHomeGoals != -1 ? newHomeGoals : (dbMatch.getHomeGoals() != null ? dbMatch.getHomeGoals() : 0);
                        int finalAwayGoals = newAwayGoals != -1 ? newAwayGoals : (dbMatch.getAwayGoals() != null ? dbMatch.getAwayGoals() : 0);

                        boolean needsSave = false;

                        if ("IN_PLAY".equals(apiStatus) || "PAUSED".equals(apiStatus) || "LIVE".equals(apiStatus) || "HALFTIME".equals(apiStatus) || "EXTRA_TIME".equals(apiStatus) || "PENALTY_SHOOTOUT".equals(apiStatus)) {
                            if (dbMatch.getStatus() == MatchStatus.PENDING) {
                                dbMatch.setStatus(MatchStatus.IN_PROGRESS);
                                dbMatch.setIsLocked(true);
                            }
                            dbMatch.setHomeGoals(finalHomeGoals);
                            dbMatch.setAwayGoals(finalAwayGoals);
                            needsSave = true;
                        } else if ("FINISHED".equals(apiStatus) || "AWARDED".equals(apiStatus)) {
                            if (dbMatch.getStatus() != MatchStatus.FINISHED) {
                                String winnerField = (liveFixture.score() != null) ? liveFixture.score().winner() : null;
                                String winningTeam = null;
                                if ("HOME_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getHomeTeam();
                                else if ("AWAY_TEAM".equalsIgnoreCase(winnerField)) winningTeam = dbMatch.getAwayTeam();
                                else if (finalHomeGoals > finalAwayGoals) winningTeam = dbMatch.getHomeTeam();
                                else if (finalAwayGoals > finalHomeGoals) winningTeam = dbMatch.getAwayTeam();

                                dbMatch.setStatus(MatchStatus.FINISHED);
                                dbMatch.setIsLocked(true);
                                dbMatch.setHomeGoals(finalHomeGoals);
                                dbMatch.setAwayGoals(finalAwayGoals);
                                dbMatch.setWinningTeam(winningTeam);
                                needsSave = true;
                                this.scoringService.scoreMatch(dbMatch);
                            }
                        }
                        if (needsSave) matchesToSave.add(dbMatch);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error con Football-Data: " + e.getMessage());
            }
        }
        if (!matchesToSave.isEmpty()) matchRepository.saveAll(matchesToSave);
    }

    private int resolveHomeGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return -1;
        if (liveFixture.score().fullTime() != null && liveFixture.score().fullTime().home() != null)
            return liveFixture.score().fullTime().home();
        return -1;
    }

    private int resolveAwayGoals(FootballDataResponse.MatchData liveFixture) {
        if (liveFixture.score() == null) return -1;
        if (liveFixture.score().fullTime() != null && liveFixture.score().fullTime().away() != null)
            return liveFixture.score().fullTime().away();
        return -1;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void lockUpcomingMatches() {
        LocalDateTime horaActualUtc = LocalDateTime.now(java.time.ZoneOffset.UTC);
        List<Match> activeMatches = matchRepository.findByStatusNot(MatchStatus.FINISHED);
        for (Match m : activeMatches) {
            if (m.getStatus() == MatchStatus.PENDING && horaActualUtc.isAfter(m.getStartTime().minusHours(1))) {
                m.setIsLocked(true);
                matchRepository.save(m);
            }
        }
    }
}
