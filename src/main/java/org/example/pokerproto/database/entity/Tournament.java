package org.example.pokerproto.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "excel_url", nullable = false, columnDefinition = "text")
    private String excelUrl;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean finished = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
