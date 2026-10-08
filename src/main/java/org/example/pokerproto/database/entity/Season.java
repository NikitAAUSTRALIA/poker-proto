package org.example.pokerproto.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@NoArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt = Instant.now();

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Season(String name) {
        this.name = name;
        this.startsAt = Instant.now();
        this.active = true;
        this.createdAt = Instant.now();
    }
}
