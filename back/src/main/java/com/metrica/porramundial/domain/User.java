package com.metrica.porramundial.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer exactMatchesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer goalDiffMatchesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer winnerMatchesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Country country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAccountActive = false;

    private String activationToken;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.isAccountActive;
    }
}