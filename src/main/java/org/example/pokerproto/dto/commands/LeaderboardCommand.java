package org.example.pokerproto.dto.commands;

import lombok.RequiredArgsConstructor;
import org.example.pokerproto.dto.LeaderboardEntryDto;
import org.example.pokerproto.service.RankingService;
import org.example.pokerproto.service.SeasonService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LeaderboardCommand implements Command {

    private final RankingService rankingService;
    private final SeasonService seasonService;

    @Override
    public String command() {
        return "/leaderboard";
    }

    @Override
    public String description() {
        return "Топ лидеров сезона";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        List<LeaderboardEntryDto> leaderboard = rankingService.getLeaderboard("season");
        String seasonName = seasonService.getActiveSeason()
                .map(s -> "«" + s.getName() + "»")
                .orElse("Общий зачет");

        if (leaderboard.isEmpty()) {
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("🏆 Рейтинг: " + seasonName + "\n\nПока нет завершенных турниров в этом сезоне.")
                    .build();
        }

        StringBuilder sb = new StringBuilder("🏆 Топ игроков: " + seasonName + "\n\n");
        int limit = Math.min(10, leaderboard.size());
        for (int i = 0; i < limit; i++) {
            LeaderboardEntryDto e = leaderboard.get(i);
            String medal;
            if (i == 0) medal = "🥇";
            else if (i == 1) medal = "🥈";
            else if (i == 2) medal = "🥉";
            else medal = (i + 1) + ".";

            String name = (e.nickname() != null && !e.nickname().isBlank())
                    ? e.nickname()
                    : (e.firstName() != null ? e.firstName() : (e.username() != null ? "@" + e.username() : "Игрок #" + e.tgId()));
            sb.append(medal).append(" ").append(name)
                    .append(" — ").append(e.points()).append(" очк.")
                    .append(" (турниров: ").append(e.tournamentsPlayed()).append(")\n");
        }

        sb.append("\nПолную таблицу и фильтры по неделе/месяцу смотри в приложении /app");

        return SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(sb.toString())
                .build();
    }
}
