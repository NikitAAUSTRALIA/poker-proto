package org.example.pokerproto.database.repository;

import org.example.pokerproto.database.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    List<Tournament> findAllByOrderByCreatedAtDesc();
}
