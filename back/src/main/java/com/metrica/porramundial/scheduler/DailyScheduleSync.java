package com.metrica.porramundial.scheduler;

import com.metrica.porramundial.domain.Match;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class DailyScheduleSync {

    private final MatchRepository matchRepository;
    private final RestClient restClient;

    public DailyScheduleSync(
            MatchRepository matchRepository,
            @Value("${api.footballdata.url}") String apiUrl,
            @Value("${api.footballdata.key}") String apiKey) {

        this.matchRepository = matchRepository;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("X-Auth-Token", apiKey)
                .build();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void syncTournamentBracket() {
        System.out.println("Sincronizando cruces del Mundial");
        try {
            FootballDataResponse apiResponse = restClient.get()
                    .uri("/competitions/WC/matches")
                    .retrieve()
                    .body(FootballDataResponse.class);

            if (apiResponse == null || apiResponse.matches() == null) {
                System.out.println("La API no devolvió partidos para sincronizar.");
                return;
            }
            List<Match> allDbMatches = matchRepository.findAll();
            for (FootballDataResponse.MatchData apiMatch : apiResponse.matches()) {
                if (apiMatch.id() == null || apiMatch.homeTeam() == null || apiMatch.awayTeam() == null) continue;
                allDbMatches.stream()
                        .filter(dbMatch -> apiMatch.id().equals(dbMatch.getApiMatchId()))
                        .findFirst()
                        .ifPresent(dbMatch -> {
                            String newHomeTeam = apiMatch.homeTeam().shortName();
                            String newAwayTeam = apiMatch.awayTeam().shortName();

                            boolean updated = false;
                            if (newHomeTeam != null && !newHomeTeam.equals(dbMatch.getHomeTeam())) {
                                dbMatch.setHomeTeam(newHomeTeam);
                                updated = true;
                            }
                            if (newAwayTeam != null && !newAwayTeam.equals(dbMatch.getAwayTeam())) {
                                dbMatch.setAwayTeam(newAwayTeam);
                                updated = true;
                            }
                            if (updated) {
                                matchRepository.save(dbMatch);
                                System.out.println("Cruce actualizado automáticamente: " + newHomeTeam + " vs " + newAwayTeam);
                            }
                        });
            }
        } catch (Exception e) {
            System.err.println("Error crítico al sincronizar cruces: " + e.getMessage());
        }
    }
}