package com.metrica.porramundial.repository;

import com.metrica.porramundial.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationToken(String token);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByTotalPointsDescExactMatchesCountDescFullNameAsc();
}