package org.example.pokerproto.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "tournament_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class TournamentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer place;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TournamentResult(Tournament tournament, Season season, User user, Integer place, Integer points) {
        this.tournament = tournament;
        this.season = season;
        this.user = user;
        this.place = place;
        this.points = points;
        this.createdAt = Instant.now();
    }
}
