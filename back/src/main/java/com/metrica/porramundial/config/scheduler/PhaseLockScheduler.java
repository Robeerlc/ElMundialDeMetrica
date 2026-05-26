package com.metrica.porramundial.config.scheduler;

import com.metrica.porramundial.domain.entity.Match;
import com.metrica.porramundial.domain.enums.TournamentPhase;
import com.metrica.porramundial.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PhaseLockScheduler {

    private final MatchRepository matchRepository;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoLockTournamentPhases() {
        for (TournamentPhase phase : TournamentPhase.values()) {
            List<Match> phaseMatches = matchRepository.findByPhase(phase);
            if (phaseMatches.isEmpty()) continue;

            boolean allAlreadyLocked = phaseMatches.stream().allMatch(Match::getIsLocked);
            if (allAlreadyLocked) continue;

            Match firstMatchOfPhase = phaseMatches.stream()
                    .min(Comparator.comparing(Match::getStartTime))
                    .orElseThrow();
            LocalDateTime lockThreshold = firstMatchOfPhase.getStartTime().minusHours(24);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(lockThreshold)) {
                for (Match match : phaseMatches) if (!match.getIsLocked()) match.setIsLocked(true);
                matchRepository.saveAll(phaseMatches);
            }
        }
    }
}