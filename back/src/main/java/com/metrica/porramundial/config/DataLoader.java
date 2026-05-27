package com.metrica.porramundial.config;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.domain.enums.MatchStatus;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.dto.FootballDataResponse;
import com.metrica.porramundial.repository.MatchRepository;
import com.metrica.porramundial.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final RestClient restClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(
            MatchRepository matchRepository,
            @Value("${api.footballdata.url}") String apiUrl,
            @Value("${api.footballdata.key}") String apiKey,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("X-Auth-Token", apiKey)
                .build();
    }

    @Override
    public void run(String @NonNull ... args) {
        cargarUsuariosVIP();
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
                                .apiMatchId(data.id())
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

    private void cargarUsuariosVIP() {
        List<String> bloqueCsv = new ArrayList<>();
        bloqueCsv.add("Email,Contraseña_Temporal");
        try (BufferedReader br = new java.io.BufferedReader(new InputStreamReader(
                new ClassPathResource("usuarios.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean esCabecera = true;
            while ((linea = br.readLine()) != null) {
                if (esCabecera) {
                    esCabecera = false;
                    continue;
                }
                String email = linea.trim();
                if (!email.isEmpty()) {
                    String passwordPlana = generarPasswordAleatoria();
                    bloqueCsv.add(email + "," + passwordPlana);
                    User user = com.metrica.porramundial.domain.entity.User.builder()
                            .email(email)
                            .password(passwordEncoder.encode(passwordPlana))
                            .requirePasswordChange(true)
                            .build();
                    userRepository.save(user);
                }
            }
            for (String fila : bloqueCsv) System.out.println(fila);
        } catch (Exception e) {
            System.err.println("Error al cargar el archivo CSV: " + e.getMessage());
        }
    }

    private String generarPasswordAleatoria() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }
}