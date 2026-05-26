package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.LeaderboardResponse;
import com.metrica.porramundial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;

    public List<LeaderboardResponse> getLeaderboard() {
        List<User> userList = userRepository.findAllByOrderByTotalPointsDescExactMatchesCountDescFullNameAsc();
        return IntStream.range(0, userList.size())
                .mapToObj(index -> {
                    User user = userList.get(index);
                    return new LeaderboardResponse(
                            index + 1,
                            user.getFullName(),
                            user.getTotalPoints(),
                            user.getExactMatchesCount(),
                            user.getGoalDiffMatchesCount(),
                            user.getWinnerMatchesCount()
                    );
                })
                .toList();
    }
}