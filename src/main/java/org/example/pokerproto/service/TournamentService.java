package org.example.pokerproto.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Registration;
import org.example.pokerproto.database.entity.RegistrationStatus;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.database.repository.RegistrationRepository;
import org.example.pokerproto.database.repository.TournamentRepository;
import org.example.pokerproto.database.repository.TournamentResultRepository;
import org.example.pokerproto.database.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepo;
    private final RegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final TournamentResultRepository resultRepo;
    private final SeasonService seasonService;

    @Transactional
    public Tournament create(String title, String excelUrl, User admin) {
        return create(title, excelUrl, null, null, admin);
    }

    @Transactional
    public Tournament create(String title, String excelUrl, Instant startsAt, Integer maxPlayers, User admin) {
        Tournament t = new Tournament();
        t.setTitle(title);
        t.setExcelUrl(excelUrl);
        t.setStartsAt(startsAt);
        t.setMaxPlayers(maxPlayers);
        t.setCreatedBy(admin);
        t.setSeason(seasonService.getActiveSeason().orElse(null));
        t.setFinished(false);
        return tournamentRepo.save(t);
    }

    public Set<Long> getRegisteredTournamentIds(User user) {
        return regRepo.findAllByUser(user).stream()
                .map(r -> r.getTournament().getId())
                .collect(Collectors.toSet());
    }

    public long getParticipantsCount(Tournament t) {
        return regRepo.countByTournamentAndStatus(t, RegistrationStatus.CONFIRMED);
    }

    public List<Tournament> listAll() {
        List<Tournament> list = new ArrayList<>(tournamentRepo.findAll());
        list.sort(Comparator.comparing(Tournament::getStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Tournament::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    public Optional<Tournament> findById(Long id) {
        return tournamentRepo.findById(id);
    }

    @Transactional
    public Registration register(Long tournamentId, Long tgId) {
        Tournament t = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + tournamentId));

        if (t.isFinished()) {
            throw new IllegalStateException("Турнир уже завершен");
        }

        User u = userRepo.findByTgId(tgId)
                .orElseThrow(() -> new IllegalStateException("Сначала напиши /register"));

        regRepo.findByTournamentAndUser(t, u).ifPresent(r -> {
            throw new IllegalStateException("Ты уже записан на этот турнир");
        });

        Registration r = new Registration();
        r.setTournament(t);
        r.setUser(u);
        r.setStatus(RegistrationStatus.CONFIRMED);
        return regRepo.save(r);
    }

    public List<Registration> listParticipants(Long tournamentId) {
        Tournament t = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден"));
        return regRepo.findAllByTournament(t);
    }

    @Transactional
    public void unregister(Long tournamentId, Long tgId) {
        Tournament t = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + tournamentId));

        if (t.isFinished()) {
            throw new IllegalStateException("Турнир уже завершен");
        }

        User u = userRepo.findByTgId(tgId)
                .orElseThrow(() -> new IllegalStateException("Сначала напиши /register"));

        Registration r = regRepo.findByTournamentAndUser(t, u)
                .orElseThrow(() -> new IllegalStateException("Ты не записан на этот турнир"));

        regRepo.delete(r);
    }

    @Transactional
    public Tournament update(Long id, String title, String excelUrl, Instant startsAt, Integer maxPlayers) {
        Tournament t = tournamentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + id));
        if (title != null && !title.isBlank()) {
            t.setTitle(title.trim());
        }
        if (excelUrl != null && !excelUrl.isBlank()) {
            t.setExcelUrl(excelUrl.trim());
        }
        t.setStartsAt(startsAt);
        t.setMaxPlayers(maxPlayers);
        return tournamentRepo.save(t);
    }

    @Transactional
    public void delete(Long id) {
        Tournament t = tournamentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + id));
        resultRepo.deleteAllByTournament(t);
        regRepo.deleteAllByTournament(t);
        tournamentRepo.delete(t);
    }

    public byte[] generateTournamentExcel(Long tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Турнир не найден: " + tournamentId));

        List<Registration> participants = regRepo.findAllByTournament(tournament);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Sheet sheet1 = workbook.createSheet("Входы");
            String[] headers1 = {"Никнейм", "Telegram ID", "Юзернейм", "Статус", "Фишки"};
            Row r0 = sheet1.createRow(0);
            for (int i = 0; i < headers1.length; i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(headers1[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx1 = 1;
            for (Registration reg : participants) {
                User u = reg.getUser();
                Row row = sheet1.createRow(rowIdx1++);
                String nick = (u.getNickname() != null && !u.getNickname().isBlank())
                        ? u.getNickname()
                        : ((u.getFirstName() != null ? u.getFirstName() : "") +
                           (u.getLastName() != null && !u.getLastName().isBlank() ? " " + u.getLastName() : ""));
                if (nick.isBlank()) {
                    nick = u.getUsername() != null ? u.getUsername() : "Игрок #" + u.getTgId();
                }

                row.createCell(0).setCellValue(nick);
                row.createCell(1).setCellValue(u.getTgId());
                row.createCell(2).setCellValue(u.getUsername() != null ? "@" + u.getUsername() : "");
                row.createCell(3).setCellValue("Ожидается");
                row.createCell(4).setCellValue(50000);
            }

            Sheet sheet2 = workbook.createSheet("Ребаи");
            String[] headers2 = {"Никнейм", "Telegram ID", "Юзернейм", "Сумма фишек"};
            Row r1 = sheet2.createRow(0);
            for (int i = 0; i < headers2.length; i++) {
                Cell c = r1.createCell(i);
                c.setCellValue(headers2[i]);
                c.setCellStyle(headerStyle);
            }

            Sheet sheet3 = workbook.createSheet("Лидеры");
            String[] headers3 = {"Никнейм", "Telegram ID", "Юзернейм", "Фишки"};
            Row r2 = sheet3.createRow(0);
            for (int i = 0; i < headers3.length; i++) {
                Cell c = r2.createCell(i);
                c.setCellValue(headers3[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx3 = 1;
            for (Registration reg : participants) {
                User u = reg.getUser();
                Row row = sheet3.createRow(rowIdx3++);
                String nick = (u.getNickname() != null && !u.getNickname().isBlank())
                        ? u.getNickname()
                        : ((u.getFirstName() != null ? u.getFirstName() : "") +
                           (u.getLastName() != null && !u.getLastName().isBlank() ? " " + u.getLastName() : ""));
                if (nick.isBlank()) {
                    nick = u.getUsername() != null ? u.getUsername() : "Игрок #" + u.getTgId();
                }
                row.createCell(0).setCellValue(nick);
                row.createCell(1).setCellValue(u.getTgId());
                row.createCell(2).setCellValue(u.getUsername() != null ? "@" + u.getUsername() : "");
                row.createCell(3).setCellValue(50000);
            }

            Sheet sheet4 = workbook.createSheet("Выбывшие");
            String[] headers4 = {"Никнейм", "Telegram ID", "Юзернейм", "Время"};
            Row r3 = sheet4.createRow(0);
            for (int i = 0; i < headers4.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers4[i]);
                c.setCellStyle(headerStyle);
            }

            for (Sheet s : List.of(sheet1, sheet2, sheet3, sheet4)) {
                for (int c = 0; c < 5; c++) {
                    s.autoSizeColumn(c);
                    s.setColumnWidth(c, Math.max(s.getColumnWidth(c), 4000));
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка создания Excel: " + e.getMessage(), e);
        }
    }
}
