package org.example.pokerproto.database.repository;

import org.example.pokerproto.database.entity.Role;
import org.example.pokerproto.database.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTgId(Long tgId);
    Optional<User> findFirstByUsernameIgnoreCase(String username);
    List<User> findAllByRole(Role role);
}
