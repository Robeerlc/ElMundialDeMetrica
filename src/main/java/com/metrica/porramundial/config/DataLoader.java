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
        System.out.println("Arrancando inyector de partidos...");
        fetchAndSaveMatches("WC", false);
        fetchAndSaveMatches("CL", true);
    }

    private void fetchAndSaveMatches(String competitionCode, boolean onlyFinal) {
        System.out.println("Buscando partidos para la competición: " + competitionCode + "...");
        try {
            FootballDataResponse apiResponse = restClient.get()
                    .uri("/competitions/" + competitionCode + "/matches")
                    .retrieve()
                    .body(FootballDataResponse.class);
            if (apiResponse != null && apiResponse.matches() != null) {
                List<Match> matchesToSave = new ArrayList<>();

                for (FootballDataResponse.MatchData data : apiResponse.matches()) {

                    if (onlyFinal && !"FINAL".equalsIgnoreCase(data.stage())) continue;
                    if (matchRepository.existsByApiMatchId(data.id())) continue;
                    if (data.homeTeam() == null || data.awayTeam() == null ||
                            data.homeTeam().shortName() == null || data.awayTeam().shortName() == null) continue;

                    String homeTeam = data.homeTeam().shortName();
                    String awayTeam = data.awayTeam().shortName();

                    ZonedDateTime zdt = ZonedDateTime.parse(data.utcDate());
                    TournamentPhase phase = mapPhase(data.stage());
                    if (phase == null) continue;

                    MatchStatus status = mapStatus(data.status());
                    Match match = Match.builder()
                            .apiMatchId(data.id())
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .startTime(zdt.toLocalDateTime())
                            .phase(phase)
                            .status(status)
                            .isLocked(status == MatchStatus.FINISHED || status == MatchStatus.IN_PROGRESS)
                            .build();
                    matchesToSave.add(match);
                }

                if (!matchesToSave.isEmpty()) {
                    matchRepository.saveAll(matchesToSave);
                    System.out.println(matchesToSave.size() + " NUEVOS partidos inyectados para " + competitionCode);
                } else {
                    System.out.println("Todos los partidos de " + competitionCode + " requeridos ya estaban en la BD.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error con Football-Data (" + competitionCode + "): " + e.getMessage());
        }
    }

    private MatchStatus mapStatus(String apiStatus) {
        if (apiStatus == null) return MatchStatus.PENDING;
        return switch (apiStatus) {
            case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
            case "IN_PLAY", "PAUSED" -> MatchStatus.IN_PROGRESS;
            default -> MatchStatus.PENDING;
        };
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