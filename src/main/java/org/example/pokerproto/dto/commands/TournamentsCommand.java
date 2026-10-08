package org.example.pokerproto.dto.commands;

import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.service.TournamentService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TournamentsCommand implements Command {

    private final TournamentService tournamentService;

    @Override
    public String command() {
        return "/tournaments";
    }

    @Override
    public String description() {
        return "Список турниров";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        var list = tournamentService.listAll();

        if (list.isEmpty()) {
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("Турниров пока нет.")
                    .build();
        }

        StringBuilder sb = new StringBuilder("Турниры:\n");
        for (Tournament t : list) {
            sb.append("#").append(t.getId())
                    .append(" — ").append(t.getTitle());
            if (t.isFinished()) {
                sb.append(" (завершен)");
            }
            sb.append("\n");
        }
        sb.append("\nЗаписаться: /tournament_reg <id>");
        return SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(sb.toString())
                .build();
    }
}
