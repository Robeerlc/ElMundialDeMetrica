package com.metrica.porramundial.config;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
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

    // 1. Se ejecuta al arrancar el servidor
    @Override
    public void run(String @NonNull ... args) {
        System.out.println("[STARTUP] Arrancando inyector de partidos...");
        this.fetchAndLoadMatches();
    }

    // 2. Se ejecuta automáticamente cada 6 horas (21600000 milisegundos)
    @Scheduled(fixedRate = 21600000)
    public void autoFetchNewPhases() {
        System.out.println("[CRON] Buscando actualización del calendario para nuevas fases...");
        this.fetchAndLoadMatches();
    }

    // El motor central que hace todo el trabajo
    private void fetchAndLoadMatches() {
        System.out.println("Buscando partidos para el Mundial");
        try {
            FootballDataResponse apiResponse = restClient.get()
                    .uri("/competitions/" + "WC" + "/matches")
                    .retrieve()
                    .body(FootballDataResponse.class);
            if (apiResponse != null && apiResponse.matches() != null) {
                List<Match> matchesToSave = new ArrayList<>();
                for (FootballDataResponse.MatchData data : apiResponse.matches()) {

                    if (matchRepository.existsByApiMatchId(data.id())) continue;
                    if (data.homeTeam() == null || data.awayTeam() == null ||
                            data.homeTeam().shortName() == null || data.awayTeam().shortName() == null) continue;

                    String homeTeam = data.homeTeam().shortName();
                    String awayTeam = data.awayTeam().shortName();

                    ZonedDateTime zdt = ZonedDateTime.parse(data.utcDate());
                    TournamentPhase phase = mapPhase(data.stage());
                    if (phase == null) continue;
                    MatchStatus status = mapStatus(data.status());

                    int resolvedHome = resolveHomeGoals(data);
                    int resolvedAway = resolveAwayGoals(data);

                    int homeG = resolvedHome != -1 ? resolvedHome : 0;
                    int awayG = resolvedAway != -1 ? resolvedAway : 0;

                    String winningTeam = null;

                    if (data.score() != null) {
                        String winnerField = data.score().winner();
                        if ("HOME_TEAM".equalsIgnoreCase(winnerField)) winningTeam = homeTeam;
                        else if ("AWAY_TEAM".equalsIgnoreCase(winnerField)) winningTeam = awayTeam;
                        else if (status == MatchStatus.FINISHED) {
                            int rawHome = data.score().fullTime() != null && data.score().fullTime().home() != null ? data.score().fullTime().home() : 0;
                            int rawAway = data.score().fullTime() != null && data.score().fullTime().away() != null ? data.score().fullTime().away() : 0;
                            if (rawHome > rawAway) winningTeam = homeTeam;
                            else if (rawAway > rawHome) winningTeam = awayTeam;
                        }
                    }

                    Match match = Match.builder()
                            .apiMatchId(data.id())
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .startTime(zdt.toLocalDateTime())
                            .phase(phase)
                            .status(status)
                            .isLocked(status == MatchStatus.FINISHED || status == MatchStatus.IN_PROGRESS)
                            .homeGoals(homeG)
                            .awayGoals(awayG)
                            .winningTeam(winningTeam)
                            .build();
                    matchesToSave.add(match);
                }

                if (!matchesToSave.isEmpty()) {
                    matchRepository.saveAll(matchesToSave);
                    System.out.println(matchesToSave.size() + " NUEVOS partidos inyectados para WC");
                } else {
                    System.out.println("Todos los partidos de WC requeridos ya estaban en la BD.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error con Football-Data (WC): " + e.getMessage());
        }
    }

    private MatchStatus mapStatus(String apiStatus) {
        if (apiStatus == null) return MatchStatus.PENDING;
        return switch (apiStatus) {
            case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
            case "IN_PLAY", "PAUSED", "LIVE", "HALFTIME", "EXTRA_TIME", "PENALTY_SHOOTOUT" -> MatchStatus.IN_PROGRESS;
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

    private int resolveHomeGoals(FootballDataResponse.MatchData data) {
        if (data.score() == null || data.score().fullTime() == null) return -1;
        return data.score().fullTime().home() != null ? data.score().fullTime().home() : 0;
    }

    private int resolveAwayGoals(FootballDataResponse.MatchData data) {
        if (data.score() == null || data.score().fullTime() == null) return -1;
        return data.score().fullTime().away() != null ? data.score().fullTime().away() : 0;
    }
}
