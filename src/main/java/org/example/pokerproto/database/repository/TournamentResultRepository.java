package org.example.pokerproto.database.repository;

import org.example.pokerproto.database.entity.Season;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.database.entity.TournamentResult;
import org.example.pokerproto.database.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TournamentResultRepository extends JpaRepository<TournamentResult, Long> {

    List<TournamentResult> findAllByTournamentOrderByPlaceAsc(Tournament tournament);

    List<TournamentResult> findAllBySeason(Season season);

    @Query("SELECT r FROM TournamentResult r WHERE r.createdAt >= :since")
    List<TournamentResult> findAllSince(@Param("since") Instant since);

    Optional<TournamentResult> findByTournamentAndUser(Tournament tournament, User user);

    void deleteAllByTournament(Tournament tournament);
}
