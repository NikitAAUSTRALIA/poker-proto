package org.example.pokerproto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.dto.commands.Command;
import org.example.pokerproto.service.RankingService;
import org.example.pokerproto.service.TournamentService;
import org.example.pokerproto.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PokerBot extends TelegramWebhookBot {

    private final String username;
    private final String botPath;
    private final String botToken;
    private final Map<String, Command> commandMap = new HashMap<>();
    private final RankingService rankingService;
    private final TournamentService tournamentService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PokerBot(@Value("${telegram.bot.username}") String username,
                    @Value("${telegram.bot.token}") String token,
                    @Value("${telegram.bot.webhook-path}") String botPath,
                    List<Command> commands,
                    RankingService rankingService,
                    TournamentService tournamentService,
                    UserService userService) {
        super(token);
        this.username = username;
        this.botToken = token;
        this.botPath = botPath;
        this.rankingService = rankingService;
        this.tournamentService = tournamentService;
        this.userService = userService;
        for (Command command : commands) {
            commandMap.put(command.command(), command);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotPath() {
        return botPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (!update.hasMessage()) return null;
        Message msg = update.getMessage();

        if (msg.hasText()) {
            String cmdKey = msg.getText().trim().split("\\s+")[0].split("@")[0];
            Command command = commandMap.get(cmdKey);
            if (command != null) {
                return command.handle(msg);
            } else {
                log.info("Unknown command: {}", msg.getText());
                return SendMessage.builder()
                        .chatId(msg.getChatId().toString())
                        .text("Указанная команда не найдена")
                        .build();
            }
        }

        if (msg.hasDocument()) {
            return handleDocumentUpload(msg);
        }

        return null;
    }

    private BotApiMethod<?> handleDocumentUpload(Message msg) {
        Long fromId = msg.getFrom() != null ? msg.getFrom().getId() : null;
        if (fromId == null || !userService.isAdmin(fromId)) {
            return SendMessage.builder()
                    .chatId(msg.getChatId().toString())
                    .text("Загрузка итогов турнира доступна только администраторам.")
                    .build();
        }

        Document doc = msg.getDocument();
        String fileName = doc.getFileName() != null ? doc.getFileName().toLowerCase() : "";
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            return SendMessage.builder()
                    .chatId(msg.getChatId().toString())
                    .text("Пожалуйста, отправьте файл в формате Excel (.xlsx) с 4-м листом «Выбывшие».")
                    .build();
        }

        Long targetId = null;
        String caption = msg.getCaption();
        if (caption != null && !caption.isBlank()) {
            String digits = caption.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                targetId = Long.parseLong(digits);
            }
        }

        Tournament targetTourney = null;
        if (targetId != null) {
            targetTourney = tournamentService.findById(targetId).orElse(null);
        }
        if (targetTourney == null) {
            targetTourney = tournamentService.listAll().stream()
                    .filter(t -> !t.isFinished())
                    .findFirst()
                    .orElse(tournamentService.listAll().stream().findFirst().orElse(null));
        }

        if (targetTourney == null) {
            return SendMessage.builder()
                    .chatId(msg.getChatId().toString())
                    .text("Турнир не найден. Укажите ID турнира в описании к файлу (например: #1).")
                    .build();
        }

        try {
            String getFileUrl = "https://api.telegram.org/bot" + botToken + "/getFile?file_id=" + doc.getFileId();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest getFileReq = HttpRequest.newBuilder().uri(URI.create(getFileUrl)).GET().build();
            HttpResponse<String> getFileResp = client.send(getFileReq, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(getFileResp.body());
            String filePath = root.path("result").path("file_path").asText(null);
            if (filePath == null) {
                return SendMessage.builder()
                        .chatId(msg.getChatId().toString())
                        .text("Не удалось получить файл от Telegram. Попробуйте еще раз.")
                        .build();
            }

            String downloadUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
            HttpRequest dlReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
            HttpResponse<InputStream> dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());

            var results = rankingService.processTournamentResults(targetTourney.getId(), dlResp.body());

            return SendMessage.builder()
                    .chatId(msg.getChatId().toString())
                    .text("✅ Итоги турнира #" + targetTourney.getId() + " («" + targetTourney.getTitle() + "») успешно обработаны!\n" +
                            "Начислены очки для " + results.size() + " участников. Таблица лидеров обновлена!")
                    .build();
        } catch (Exception e) {
            log.error("Error processing excel document in bot: {}", e.getMessage(), e);
            return SendMessage.builder()
                    .chatId(msg.getChatId().toString())
                    .text("❌ Ошибка обработки таблицы: " + e.getMessage())
                    .build();
        }
    }
}
