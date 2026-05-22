package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.domain.MatchStatus;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.dto.MatchResultRequest;
import com.metrica.porramundial.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class LiveMatchUpdaterService {

    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final RestClient restClient;

    public LiveMatchUpdaterService(MatchRepository matchRepository, MatchService matchService, @Value("${api.footballdata.url}") String apiUrl, @Value("${api.footballdata.key}") String apiKey) {

        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.restClient = RestClient.builder().baseUrl(apiUrl).defaultHeader("X-Auth-Token", apiKey).build();
    }

    @Scheduled(fixedRate = 300000)
    public void fetchLiveResults() {
        List<Match> liveMatches = matchRepository.findByStatus(MatchStatus.IN_PROGRESS);
        if (liveMatches.isEmpty()) return;

        try {
            FootballDataResponse apiResponse = restClient.get()
                    .uri("/competitions/WC/matches?status=IN_PLAY,PAUSED,FINISHED")
                    .retrieve()
                    .body(FootballDataResponse.class);

            if (apiResponse == null || apiResponse.matches() == null) return;
            for (FootballDataResponse.MatchData liveFixture : apiResponse.matches()) {

                if (liveFixture.homeTeam() == null || liveFixture.awayTeam() == null) continue;

                String apiHomeTeam = liveFixture.homeTeam().shortName();
                String apiAwayTeam = liveFixture.awayTeam().shortName();

                for (Match dbMatch : liveMatches) {
                    if (dbMatch.getHomeTeam().equalsIgnoreCase(apiHomeTeam) && dbMatch.getAwayTeam().equalsIgnoreCase(apiAwayTeam)) {

                        int homeGoals = 0;
                        int awayGoals = 0;

                        if (liveFixture.score() != null && liveFixture.score().fullTime() != null) {
                            homeGoals = liveFixture.score().fullTime().home() != null ? liveFixture.score().fullTime().home() : 0;
                            awayGoals = liveFixture.score().fullTime().away() != null ? liveFixture.score().fullTime().away() : 0;
                        }

                        if ("FINISHED".equals(liveFixture.status())) {
                            String winningTeam = null;
                            if (homeGoals > awayGoals) winningTeam = apiHomeTeam;
                            else if (awayGoals > homeGoals) winningTeam = apiAwayTeam;
                            matchService.updateMatchResult(dbMatch.getId(), new MatchResultRequest(homeGoals, awayGoals, winningTeam));
                        } else {
                            dbMatch.setHomeGoals(homeGoals);
                            dbMatch.setAwayGoals(awayGoals);
                            matchRepository.save(dbMatch);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al contactar con Football-Data: " + e.getMessage());
        }
    }
}