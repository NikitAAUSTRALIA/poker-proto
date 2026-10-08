package org.example.pokerproto.service;

import lombok.extern.slf4j.Slf4j;
import org.example.pokerproto.database.entity.Role;
import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.database.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository repo;
    private final Set<Long> adminIds;

    public UserService(UserRepository repo,
                       @Value("${app.admin-tg-ids:}") String adminIdsRaw) {
        this.repo = repo;
        this.adminIds = new java.util.HashSet<>(Arrays.stream(adminIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet()));
        log.info("Admin tg ids: {}", adminIds);
    }

    @Transactional
    public User registerOrUpdate(Long tgId, String username, String firstName, String lastName) {
        User user = repo.findByTgId(tgId).orElseGet(User::new);
        user.setTgId(tgId);
        if (username != null && username.startsWith("@")) {
            username = username.substring(1);
        }
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (adminIds.contains(tgId)) {
            user.setRole(Role.ADMIN);
        } else if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        return repo.save(user);
    }

    public Optional<User> findByTgId(Long tgId) {
        return repo.findByTgId(tgId);
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String clean = username.startsWith("@") ? username.substring(1).trim() : username.trim();
        return repo.findFirstByUsernameIgnoreCase(clean);
    }

    @Transactional
    public boolean isAdmin(Long tgId) {
        if (tgId == null) {
            return false;
        }
        if (adminIds.contains(tgId)) {
            repo.findByTgId(tgId).ifPresent(u -> {
                if (u.getRole() != Role.ADMIN) {
                    u.setRole(Role.ADMIN);
                    repo.save(u);
                }
            });
            return true;
        }
        return repo.findByTgId(tgId)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);
    }

    @Transactional
    public User requireAdmin(Long tgId) {
        User user = repo.findByTgId(tgId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + tgId));
        if (adminIds.contains(tgId) || user.getRole() == Role.ADMIN) {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                repo.save(user);
            }
            return user;
        }
        throw new IllegalStateException("Not an admin: " + tgId);
    }

    @Transactional
    public boolean grantAdmin(Long targetTgId) {
        if (targetTgId == null) {
            return false;
        }
        adminIds.add(targetTgId);
        User user = repo.findByTgId(targetTgId).orElseGet(() -> {
            User u = new User();
            u.setTgId(targetTgId);
            return u;
        });
        user.setRole(Role.ADMIN);
        repo.save(user);
        log.info("Granted admin rights to tg_id: {}", targetTgId);
        return true;
    }

    @Transactional
    public boolean ungrantAdmin(Long targetTgId) {
        if (targetTgId == null) {
            return false;
        }
        adminIds.remove(targetTgId);
        repo.findByTgId(targetTgId).ifPresent(u -> {
            u.setRole(Role.USER);
            repo.save(u);
        });
        log.info("Revoked admin rights from tg_id: {}", targetTgId);
        return true;
    }
}
