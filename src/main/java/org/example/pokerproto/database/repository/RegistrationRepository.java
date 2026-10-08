package org.example.pokerproto.database.repository;

import org.example.pokerproto.database.entity.Registration;
import org.example.pokerproto.database.entity.RegistrationStatus;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.database.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByTournamentAndUser(Tournament t, User u);
    List<Registration> findAllByTournament(Tournament t);
    List<Registration> findAllByUser(User u);
    long countByTournamentAndStatus(Tournament t, RegistrationStatus status);
    void deleteAllByTournament(Tournament t);
}
