package org.example.pokerproto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pokerproto.database.entity.Season;
import org.example.pokerproto.database.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepo;

    public Optional<Season> getActiveSeason() {
        return seasonRepo.findFirstByActiveTrueOrderByStartsAtDesc();
    }

    public List<Season> listAll() {
        return seasonRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Season startSeason(String name) {
        String cleanName = (name != null && !name.isBlank()) ? name.trim() : "Новый сезон";
        seasonRepo.findFirstByActiveTrueOrderByStartsAtDesc().ifPresent(cur -> {
            cur.setActive(false);
            cur.setEndsAt(Instant.now());
            seasonRepo.save(cur);
            log.info("Finished previous season: {} (ID: {})", cur.getName(), cur.getId());
        });

        Season newSeason = new Season(cleanName);
        Season saved = seasonRepo.save(newSeason);
        log.info("Started new active season: {} (ID: {})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Season endSeason() {
        Season active = seasonRepo.findFirstByActiveTrueOrderByStartsAtDesc()
                .orElseThrow(() -> new IllegalStateException("Сейчас нет активного сезона"));
        active.setActive(false);
        active.setEndsAt(Instant.now());
        Season saved = seasonRepo.save(active);
        log.info("Ended active season: {} (ID: {})", saved.getName(), saved.getId());
        return saved;
    }
}
