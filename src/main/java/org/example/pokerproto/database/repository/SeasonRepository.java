package org.example.pokerproto.database.repository;

import org.example.pokerproto.database.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findFirstByActiveTrueOrderByStartsAtDesc();
    List<Season> findAllByOrderByCreatedAtDesc();
}
