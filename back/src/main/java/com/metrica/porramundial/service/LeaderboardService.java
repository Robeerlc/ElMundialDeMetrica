package com.metrica.porramundial.service;

import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.LeaderboardResponse;
import com.metrica.porramundial.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {
    private final UserRepository userRepository;

    private static LeaderboardResponse userAndPositionToLeaderboardResponse(UserAndPosition uap) {
        return new LeaderboardResponse(
                uap.position,
                uap.user.getFullName(),
                uap.user.getTotalPoints(),
                uap.user.getExactMatchesCount(),
                uap.user.getGoalDiffMatchesCount(),
                uap.user.getWinnerMatchesCount()
        );
    }

    public List<LeaderboardResponse> getLeaderboard() {
        return this.getOrderedUsers()
                .map(LeaderboardService::userAndPositionToLeaderboardResponse)
                .toList();
    }

    public Optional<LeaderboardResponse> getMyLeaderboard(String email) {
        return this.getOrderedUsers()
                .filter(uap -> uap.user().getEmail().equals(email))
                .findFirst()
                .map(LeaderboardService::userAndPositionToLeaderboardResponse);
    }

    private Stream<UserAndPosition> getOrderedUsers() {
        List<User> userList = userRepository.findAllByOrderByTotalPointsDescExactMatchesCountDescFullNameAsc();
        return IntStream.range(0, userList.size())
                .mapToObj(index -> {
                    User user = userList.get(index);
                    return new UserAndPosition(user, index + 1);
                });
    }

    private record UserAndPosition(User user, int position) {
    }
}