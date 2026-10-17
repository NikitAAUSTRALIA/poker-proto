package org.example.pokerproto.controller;

import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Season;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.dto.*;
import org.example.pokerproto.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final UserService userService;
    private final TvService tvService;
    private final SeasonService seasonService;
    private final RankingService rankingService;
    private final InitDataValidator validator;

    @GetMapping("/user/me")
    public UserProfileDto me(@RequestHeader("X-Init-Data") String initData) {
        var tgUser = validator.extractTelegramUser(initData);
        User user = userService.findByTgId(tgUser.id())
                .orElseGet(() -> userService.registerOrUpdate(
                        tgUser.id(),
                        tgUser.username(),
                        tgUser.firstName(),
                        tgUser.lastName()
                ));
        boolean isAdmin = userService.isAdmin(user.getTgId());
        return new UserProfileDto(user.getTgId(), user.getUsername(), user.getFirstName(), isAdmin, user.isTermsAgreed());
    }

    @PostMapping("/user/register")
    public UserProfileDto registerInApp(@RequestHeader("X-Init-Data") String initData) {
        var tgUser = validator.extractTelegramUser(initData);
        User user = userService.registerOrUpdate(
                tgUser.id(),
                tgUser.username(),
                tgUser.firstName(),
                tgUser.lastName()
        );
        user = userService.agreeTerms(user.getTgId());
        boolean isAdmin = userService.isAdmin(user.getTgId());
        return new UserProfileDto(user.getTgId(), user.getUsername(), user.getFirstName(), isAdmin, user.isTermsAgreed());
    }

    @GetMapping("/tournaments")
    public List<TournamentDto> list(@RequestHeader("X-Init-Data") String initData) {
        User user = requireUser(initData);
        Set<Long> registeredIds = tournamentService.getRegisteredTournamentIds(user);
        return tournamentService.listAll().stream()
                .map(t -> toDto(t, registeredIds.contains(t.getId())))
                .toList();
    }

    @GetMapping("/tv")
    public ResponseEntity<Void> tvRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/tv.html"))
                .build();
    }

    @GetMapping("/public/tournaments")
    public List<TournamentDto> listPublic() {
        return tournamentService.listAll().stream()
                .map(t -> toDto(t, false))
                .toList();
    }

    @GetMapping("/public/tournaments/{id}")
    public TournamentDto getPublic(@PathVariable Long id) {
        Tournament t = tournamentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Турнир не найден"));
        return toDto(t, false);
    }

    @GetMapping("/public/leaderboard")
    public List<LeaderboardEntryDto> getLeaderboard(@RequestParam(value = "period", defaultValue = "season") String period) {
        return rankingService.getLeaderboard(period);
    }

    @GetMapping("/public/seasons/active")
    public ResponseEntity<SeasonDto> getActiveSeason() {
        return seasonService.getActiveSeason()
                .map(s -> ResponseEntity.ok(new SeasonDto(s.getId(), s.getName(), s.getStartsAt(), s.getEndsAt(), s.isActive())))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/public/seasons")
    public List<SeasonDto> listSeasons() {
        return seasonService.listAll().stream()
                .map(s -> new SeasonDto(s.getId(), s.getName(), s.getStartsAt(), s.getEndsAt(), s.isActive()))
                .toList();
    }

    @PostMapping("/admin/seasons/start")
    public SeasonDto startSeason(@RequestBody Map<String, String> body,
                                 @RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);
        String name = body != null ? body.get("name") : null;
        Season season = seasonService.startSeason(name);
        return new SeasonDto(season.getId(), season.getName(), season.getStartsAt(), season.getEndsAt(), season.isActive());
    }

    @PostMapping("/admin/seasons/end")
    public SeasonDto endSeason(@RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);
        Season season = seasonService.endSeason();
        return new SeasonDto(season.getId(), season.getName(), season.getStartsAt(), season.getEndsAt(), season.isActive());
    }

    @PostMapping(value = "/admin/tournaments/{id}/results/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadTournamentResults(@PathVariable Long id,
                                                                      @RequestParam("file") MultipartFile file,
                                                                      @RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл не должен быть пустым");
        }
        try {
            var results = rankingService.processTournamentResults(id, file.getInputStream());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "processedPlayers", results.size(),
                    "message", "Итоги турнира успешно сохранены, очки начислены в рейтинг!"
            ));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ошибка обработки Excel: " + e.getMessage());
        }
    }

    @PostMapping("/admin/tournaments/{id}/results/from-url")
    public ResponseEntity<Map<String, Object>> processResultsFromUrl(@PathVariable Long id,
                                                                    @RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);
        Tournament t = tournamentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Турнир не найден"));

        String excelUrl = t.getExcelUrl();
        if (excelUrl == null || excelUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У турнира не указана ссылка на таблицу");
        }

        try {
            byte[] fileBytes = fetchExcelBytes(excelUrl);
            var results = rankingService.processTournamentResults(id, new ByteArrayInputStream(fileBytes));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "processedPlayers", results.size(),
                    "message", "Итоги турнира успешно загружены по ссылке и начислены в рейтинг!"
            ));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось загрузить таблицу: " + e.getMessage());
        }
    }

    @GetMapping({"/public/proxy-excel", "/api/public/proxy-excel"})
    public ResponseEntity<byte[]> proxyExcel(@RequestParam("url") String url) {
        try {
            byte[] bytes = fetchExcelBytes(url);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(bytes);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Не удалось загрузить таблицу: " + e.getMessage());
        }
    }

    private byte[] fetchExcelBytes(String url) throws Exception {
        String targetUrl = url != null ? url.trim() : "";
        if (targetUrl.contains("docs.google.com/spreadsheets") && !targetUrl.contains("export?format=")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/d/([a-zA-Z0-9-_]+)").matcher(targetUrl);
            if (matcher.find()) {
                targetUrl = "https://docs.google.com/spreadsheets/d/" + matcher.group(1) + "/export?format=xlsx";
            }
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Google Таблица закрыта настройками приватности. Включите доступ: «Поделиться» -> «Все, у кого есть ссылка — Читатель».");
        }
        return response.body();
    }

    @PostMapping("/tournaments")
    public TournamentDto create(@RequestBody SaveTournamentRequest request,
                                @RequestHeader("X-Init-Data") String initData) {
        User user = requireAdmin(initData);
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название турнира");
        }
        if (request.excelUrl() == null || request.excelUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите ссылку на таблицу");
        }

        Tournament created = tournamentService.create(
                request.title().trim(),
                request.excelUrl().trim(),
                request.startsAt(),
                request.maxPlayers(),
                user
        );

        return toDto(created, false);
    }

    @PutMapping("/tournaments/{id}")
    public TournamentDto update(@PathVariable Long id,
                                @RequestBody SaveTournamentRequest request,
                                @RequestHeader("X-Init-Data") String initData) {
        User user = requireAdmin(initData);
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите название турнира");
        }
        if (request.excelUrl() == null || request.excelUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите ссылку на таблицу");
        }

        Tournament updated = tournamentService.update(
                id,
                request.title(),
                request.excelUrl(),
                request.startsAt(),
                request.maxPlayers()
        );

        Set<Long> registeredIds = tournamentService.getRegisteredTournamentIds(user);
        return toDto(updated, registeredIds.contains(updated.getId()));
    }

    @PostMapping("/tournaments/{id}/register")
    public ResponseEntity<String> register(@PathVariable Long id,
                                           @RequestHeader("X-Init-Data") String initData) {
        User user = requireUser(initData);
        try {
            tournamentService.register(id, user.getTgId());
            return ResponseEntity.ok("Ты записан");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/tournaments/{id}/unregister")
    public ResponseEntity<String> unregister(@PathVariable Long id,
                                             @RequestHeader("X-Init-Data") String initData) {
        User user = requireUser(initData);
        try {
            tournamentService.unregister(id, user.getTgId());
            return ResponseEntity.ok("Запись отменена");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/tournaments/{id}")
    public void deleteTournament(@PathVariable Long id,
                                 @RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);
        tournamentService.delete(id);
    }

    @GetMapping({"/tournaments/{id}/participants", "/public/tournaments/{id}/participants"})
    public List<ParticipantDto> listParticipants(@PathVariable Long id) {
        return tournamentService.listParticipants(id).stream()
                .map(r -> new ParticipantDto(
                        r.getUser().getTgId(),
                        r.getUser().getUsername(),
                        r.getUser().getFirstName(),
                        r.getUser().getLastName(),
                        r.getStatus().name(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping(value = {"/tournaments/{id}/export-excel", "/public/tournaments/{id}/export-excel"},
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportTournamentExcel(@PathVariable Long id) {
        Tournament t = tournamentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Турнир не найден"));

        byte[] excelBytes = tournamentService.generateTournamentExcel(id);
        String safeTitle = (t.getTitle() != null ? t.getTitle() : "tournament")
                .replaceAll("[^a-zA-Zа-яА-Я0-9_-]", "_");
        String filename = "tournament-" + id + "-" + safeTitle + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }

    @GetMapping("/public/tv/state")
    public TvService.TvState getTvState() {
        return tvService.getState();
    }

    @PostMapping("/admin/tv/action")
    public TvService.TvState handleTvAction(@RequestBody TvActionRequest request,
                                           @RequestHeader("X-Init-Data") String initData) {
        requireAdmin(initData);

        String action = request.action() != null ? request.action().toUpperCase() : "";
        switch (action) {
            case "SET_TOURNAMENT":
                if (request.tournamentId() != null) {
                    return tvService.setTournament(request.tournamentId());
                }
                break;
            case "START":
                return tvService.startTimer();
            case "PAUSE":
                return tvService.pauseTimer();
            case "NEXT_LEVEL":
                return tvService.nextLevel(request.secondsLeft() != null ? request.secondsLeft() : 20 * 60);
            case "PREV_LEVEL":
                return tvService.prevLevel(request.secondsLeft() != null ? request.secondsLeft() : 20 * 60);
            case "TOGGLE_BREAK":
                return tvService.toggleBreak(request.secondsLeft() != null ? request.secondsLeft() : 15 * 60);
            case "ADJUST_TIME":
                return tvService.adjustTime(request.deltaSeconds() != null ? request.deltaSeconds() : 60);
            case "SET_BLINDS":
                return tvService.setBlinds(request.bigBlind(), request.smallBlind(), request.ante());
            case "SYNC":
                return tvService.syncCustom(request.level(), request.secondsLeft(), request.isBreak(), null);
            default:
                break;
        }
        return tvService.getState();
    }

    private TournamentDto toDto(Tournament t, boolean registered) {
        String seasonName = t.getSeason() != null ? t.getSeason().getName() : null;
        return new TournamentDto(
                t.getId(),
                t.getTitle(),
                t.getExcelUrl(),
                t.getStartsAt(),
                t.getMaxPlayers(),
                tournamentService.getParticipantsCount(t),
                registered,
                t.isFinished(),
                seasonName
        );
    }

    private User requireUser(String initData) {
        var tgUser = validator.extractTelegramUser(initData);
        return userService.findByTgId(tgUser.id())
                .orElseGet(() -> userService.registerOrUpdate(
                        tgUser.id(),
                        tgUser.username(),
                        tgUser.firstName(),
                        tgUser.lastName()
                ));
    }

    private User requireAdmin(String initData) {
        User user = requireUser(initData);
        if (!userService.isAdmin(user.getTgId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ только для администраторов");
        }
        return user;
    }
}
