package org.example.pokerproto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.example.pokerproto.database.entity.*;
import org.example.pokerproto.database.repository.*;
import org.example.pokerproto.dto.LeaderboardEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final TournamentResultRepository resultRepo;
    private final TournamentRepository tournamentRepo;
    private final UserRepository userRepo;
    private final SeasonService seasonService;
    private final RegistrationRepository registrationRepo;

    public int calculatePoints(int place, int totalPlayers) {
        if (place <= 0 || totalPlayers <= 0) return 0;
        int safePlace = Math.min(place, totalPlayers);
        double placeFactor = Math.pow((double) (totalPlayers - safePlace + 1) / totalPlayers, 1.8);
        double basePoints = 15.0;
        double maxScale = 50.0 * Math.sqrt(totalPlayers);
        return (int) Math.round(basePoints + maxScale * placeFactor);
    }

    @Transactional
    public List<TournamentResult> processTournamentResults(Long tournamentId, InputStream excelInputStream) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + tournamentId));

        Season activeSeason = seasonService.getActiveSeason().orElse(null);
        if (tournament.getSeason() == null && activeSeason != null) {
            tournament.setSeason(activeSeason);
        }

        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {

            List<Sheet1PlayerRecord> sheet1Players = parseSheet1(workbook);
            Set<Long> arrivedTgIds = new LinkedHashSet<>();
            Set<Long> noShowTgIds = new LinkedHashSet<>();

            for (Sheet1PlayerRecord p : sheet1Players) {
                if (p.tgId() == null) continue;
                if (isArrivedStatus(p.status())) {
                    arrivedTgIds.add(p.tgId());
                } else {
                    noShowTgIds.add(p.tgId());
                }
            }

            for (Long noShowId : noShowTgIds) {
                userRepo.findByTgId(noShowId).ifPresent(u -> {
                    registrationRepo.findByTournamentAndUser(tournament, u).ifPresent(reg -> {
                        reg.setStatus(RegistrationStatus.CANCELLED);
                        registrationRepo.save(reg);
                    });
                });
            }

            List<PlayerEliminationRecord> eliminationRecords = parseSheet4(workbook);

            List<PlayerEliminationRecord> finalPlayingOrder = new ArrayList<>();
            Set<Long> alreadyAddedTgIds = new HashSet<>();

            for (PlayerEliminationRecord rec : eliminationRecords) {
                if (rec.tgId() != null) {
                    arrivedTgIds.add(rec.tgId());
                    alreadyAddedTgIds.add(rec.tgId());
                }
                finalPlayingOrder.add(rec);
            }

            for (Sheet1PlayerRecord p : sheet1Players) {
                if (p.tgId() != null && arrivedTgIds.contains(p.tgId()) && !alreadyAddedTgIds.contains(p.tgId())) {
                    finalPlayingOrder.add(new PlayerEliminationRecord(p.tgId(), p.name(), p.username()));
                    alreadyAddedTgIds.add(p.tgId());
                }
            }

            if (finalPlayingOrder.isEmpty()) {
                throw new IllegalArgumentException("В таблице не найдено пришедших участников для начисления очков");
            }

            int totalPlayers = finalPlayingOrder.size();

            resultRepo.deleteAllByTournament(tournament);

            List<TournamentResult> savedResults = new ArrayList<>();

            for (int i = 0; i < totalPlayers; i++) {
                PlayerEliminationRecord rec = finalPlayingOrder.get(i);
                int place = totalPlayers - i;
                int points = calculatePoints(place, totalPlayers);

                User user = userRepo.findByTgId(rec.tgId()).orElseGet(() -> {
                    User u = new User();
                    u.setTgId(rec.tgId());
                    u.setUsername(rec.username());
                    u.setFirstName(rec.name() != null ? rec.name() : "Игрок #" + rec.tgId());
                    u.setRole(Role.USER);
                    return userRepo.save(u);
                });

                if (rec.name() != null && (user.getFirstName() == null || user.getFirstName().startsWith("Игрок #"))) {
                    user.setFirstName(rec.name());
                    userRepo.save(user);
                }
                if (rec.username() != null && user.getUsername() == null) {
                    user.setUsername(rec.username());
                    userRepo.save(user);
                }

                registrationRepo.findByTournamentAndUser(tournament, user).ifPresent(reg -> {
                    reg.setStatus(RegistrationStatus.CONFIRMED);
                    registrationRepo.save(reg);
                });

                TournamentResult tr = new TournamentResult(tournament, tournament.getSeason(), user, place, points);
                savedResults.add(resultRepo.save(tr));
            }

            tournament.setFinished(true);
            tournamentRepo.save(tournament);

            log.info("Successfully processed {} tournament results for tournament ID: {}", savedResults.size(), tournamentId);
            return savedResults;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            log.error("Error processing tournament results from excel: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка обработки результатов: " + e.getMessage(), e);
        }
    }

    public List<LeaderboardEntryDto> getLeaderboard(String period) {
        String p = (period != null) ? period.trim().toLowerCase() : "season";
        List<TournamentResult> results;

        switch (p) {
            case "week":
                Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
                results = resultRepo.findAllSince(weekAgo);
                break;
            case "month":
                Instant monthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
                results = resultRepo.findAllSince(monthAgo);
                break;
            case "season":
            default:
                Optional<Season> activeSeason = seasonService.getActiveSeason();
                if (activeSeason.isPresent()) {
                    results = resultRepo.findAllBySeason(activeSeason.get());
                } else {
                    results = resultRepo.findAll();
                }
                break;
        }

        Map<User, List<TournamentResult>> userMap = results.stream()
                .collect(Collectors.groupingBy(TournamentResult::getUser));

        List<LeaderboardEntryDto> entries = new ArrayList<>();

        for (Map.Entry<User, List<TournamentResult>> entry : userMap.entrySet()) {
            User u = entry.getKey();
            List<TournamentResult> userResults = entry.getValue();

            int totalPoints = userResults.stream().mapToInt(TournamentResult::getPoints).sum();
            int played = userResults.size();
            int wins = (int) userResults.stream().filter(r -> r.getPlace() == 1).count();
            int bestPlace = userResults.stream().mapToInt(TournamentResult::getPlace).min().orElse(999);

            entries.add(new LeaderboardEntryDto(
                    0,
                    u.getTgId(),
                    u.getUsername(),
                    u.getFirstName(),
                    u.getLastName(),
                    totalPoints,
                    played,
                    wins,
                    bestPlace,
                    u.getNickname()
            ));
        }

        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.points(), a.points());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.bestPlace(), b.bestPlace());
            if (cmp != 0) return cmp;
            return Integer.compare(b.tournamentsPlayed(), a.tournamentsPlayed());
        });

        List<LeaderboardEntryDto> rankedEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDto e = entries.get(i);
            rankedEntries.add(new LeaderboardEntryDto(
                    i + 1,
                    e.tgId(),
                    e.username(),
                    e.firstName(),
                    e.lastName(),
                    e.points(),
                    e.tournamentsPlayed(),
                    e.wins(),
                    e.bestPlace(),
                    e.nickname()
            ));
        }

        return rankedEntries;
    }

    public record Sheet1PlayerRecord(Long tgId, String name, String username, String status, Double chips) {}
    public record PlayerEliminationRecord(Long tgId, String name, String username) {}

    private List<Sheet1PlayerRecord> parseSheet1(Workbook workbook) {
        List<Sheet1PlayerRecord> records = new ArrayList<>();
        if (workbook == null || workbook.getNumberOfSheets() == 0) return records;
        Sheet sheet = workbook.getSheetAt(0);

        int idCol = -1;
        int nameCol = -1;
        int usernameCol = -1;
        int statusCol = -1;
        int chipsCol = -1;

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell c : headerRow) {
                String val = getCellString(c).toLowerCase().trim();
                if (val.contains("id") || val.contains("айди") || val.contains("tg")) {
                    idCol = c.getColumnIndex();
                } else if (val.contains("ник") || val.contains("username")) {
                    usernameCol = c.getColumnIndex();
                } else if (val.contains("имя") || val.contains("игрок") || val.contains("name")) {
                    nameCol = c.getColumnIndex();
                } else if (val.contains("статус") || val.contains("явк") || val.contains("status")) {
                    statusCol = c.getColumnIndex();
                } else if (val.contains("фишк") || val.contains("стек") || val.contains("chip")) {
                    chipsCol = c.getColumnIndex();
                }
            }
        }

        if (nameCol == -1) nameCol = 0;
        if (idCol == -1) idCol = 1;
        if (usernameCol == -1) usernameCol = 2;
        if (statusCol == -1) statusCol = 3;
        if (chipsCol == -1) chipsCol = 4;

        int startRow = (headerRow != null) ? 1 : 0;
        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String name = getCellString(row.getCell(nameCol));
            Long tgId = parseLongSafe(getCellString(row.getCell(idCol)));
            String username = getCellString(row.getCell(usernameCol));
            if (username.startsWith("@")) username = username.substring(1);
            String status = getCellString(row.getCell(statusCol));
            Double chips = parseDoubleSafe(getCellString(row.getCell(chipsCol)));

            if (tgId == null && (name == null || name.isBlank())) continue;

            records.add(new Sheet1PlayerRecord(tgId, name, username, status, chips));
        }
        return records;
    }

    private boolean isArrivedStatus(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String s = status.toLowerCase().trim();
        if (s.contains("не явился") || s.contains("не пришел") || s.contains("не был") ||
            s.contains("ожидается") || s.contains("зарег") || s.contains("отмена") ||
            s.equals("нет") || s.equals("-")) {
            return false;
        }
        return true;
    }

    private List<PlayerEliminationRecord> parseSheet4(Workbook workbook) {
        List<PlayerEliminationRecord> records = new ArrayList<>();
        if (workbook == null || workbook.getNumberOfSheets() == 0) return records;

        Sheet sheet = null;
        if (workbook.getNumberOfSheets() >= 4) {
            sheet = workbook.getSheetAt(3);
        } else {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                String name = workbook.getSheetName(s).toLowerCase();
                if (name.contains("выбыв") || name.contains("итог") || name.contains("результат") || name.contains("out")) {
                    sheet = workbook.getSheetAt(s);
                    break;
                }
            }
        }
        if (sheet == null) {
            sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
        }

        int idCol = -1;
        int nameCol = -1;
        int usernameCol = -1;

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell c : headerRow) {
                String val = getCellString(c).toLowerCase().trim();
                if (val.contains("id") || val.contains("айди") || val.contains("tg")) {
                    idCol = c.getColumnIndex();
                } else if (val.contains("ник") || val.contains("username")) {
                    usernameCol = c.getColumnIndex();
                } else if (val.contains("имя") || val.contains("игрок") || val.contains("name")) {
                    nameCol = c.getColumnIndex();
                }
            }
        }

        if (nameCol == -1) nameCol = 0;
        if (idCol == -1) idCol = 1;
        if (usernameCol == -1) usernameCol = 2;

        int startRow = (headerRow != null) ? 1 : 0;
        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Long tgId = parseLongSafe(getCellString(row.getCell(idCol)));
            String name = getCellString(row.getCell(nameCol));
            String username = getCellString(row.getCell(usernameCol));
            if (username.startsWith("@")) username = username.substring(1);

            if (tgId == null) {
                for (Cell c : row) {
                    Long parsed = parseLongSafe(getCellString(c));
                    if (parsed != null && parsed > 1000) {
                        tgId = parsed;
                        break;
                    }
                }
            }

            if (tgId == null && (name == null || name.isBlank())) continue;

            records.add(new PlayerEliminationRecord(tgId, name, username));
        }
        return records;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter df = new DataFormatter();
        return df.formatCellValue(cell).trim();
    }

    private static Long parseLongSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {

            if (str.toUpperCase().contains("E")) {
                double d = Double.parseDouble(str);
                return (long) Math.round(d);
            }
            String digits = str.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;
            return Long.parseLong(digits);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
