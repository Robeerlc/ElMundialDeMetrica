package com.metrica.porramundial.config;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final RestClient restClient;

    public DataLoader(
            MatchRepository matchRepository,
            @Value("${api.footballdata.url}") String apiUrl,
            @Value("${api.footballdata.key}") String apiKey) {

        this.matchRepository = matchRepository;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("X-Auth-Token", apiKey)
                .build();
    }

    @Override
    public void run(String @NonNull ... args) {
        if (matchRepository.count() == 0) {
            System.out.println("Conectando a Football-Data.org...");
            try {
                FootballDataResponse apiResponse = restClient.get()
                        .uri("/competitions/WC/matches")
                        .retrieve()
                        .body(FootballDataResponse.class);

                if (apiResponse != null && apiResponse.matches() != null) {
                    List<Match> matchesToSave = new ArrayList<>();

                    for (FootballDataResponse.MatchData data : apiResponse.matches()) {
                        if (data.homeTeam() == null || data.awayTeam() == null ||
                                data.homeTeam().shortName() == null || data.awayTeam().shortName() == null) continue;

                        String homeTeam = data.homeTeam().shortName();
                        String awayTeam = data.awayTeam().shortName();

                        ZonedDateTime zdt = ZonedDateTime.parse(data.utcDate());
                        TournamentPhase phase = mapPhase(data.stage());
                        if (phase == null) continue;
                        Match match = Match.builder()
                                .homeTeam(homeTeam)
                                .awayTeam(awayTeam)
                                .startTime(zdt.toLocalDateTime())
                                .phase(phase)
                                .status(MatchStatus.PENDING)
                                .isLocked(false)
                                .build();
                        matchesToSave.add(match);
                    }
                    matchRepository.saveAll(matchesToSave);
                    System.out.println("DataLoader: " + matchesToSave.size() + " partidos del Mundial inyectados con éxito");
                }
            } catch (Exception e) {
                System.err.println("Error con Football-Data: " + e.getMessage());
            }
        }
    }

    private TournamentPhase mapPhase(String apiRound) {
        if (apiRound == null) return TournamentPhase.GROUP_STAGE;
        return switch (apiRound) {
            case "GROUP_STAGE" -> TournamentPhase.GROUP_STAGE;
            case "LAST_16" -> TournamentPhase.ROUND_OF_16;
            case "QUARTER_FINALS" -> TournamentPhase.QUARTER_FINALS;
            case "SEMI_FINALS" -> TournamentPhase.SEMI_FINALS;
            case "THIRD_PLACE" -> TournamentPhase.THIRD_PLACE;
            case "FINAL" -> TournamentPhase.FINAL;
            default -> null;
        };
    }
}