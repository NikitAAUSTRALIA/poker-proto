package org.example.pokerproto.dto.commands;

import lombok.RequiredArgsConstructor;
import org.example.pokerproto.service.TournamentService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class TournamentUnregCommand implements Command {

    private final TournamentService tournamentService;

    @Override
    public String command() {
        return "/tournament_unreg";
    }

    @Override
    public String description() {
        return "Отписаться от турнира: /tournament_unreg <id>";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        var from = message.getFrom();

        String text = message.getText().trim();
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return reply(message.getChatId(), "Использование: /tournament_unreg <id>");
        }

        Long tournamentId;
        try {
            tournamentId = Long.parseLong(parts[1].trim());
        } catch (NumberFormatException e) {
            return reply(message.getChatId(), "ID турнира — это число.");
        }

        try {
            tournamentService.unregister(tournamentId, from.getId());
            return reply(message.getChatId(), "Ты отписан от турнира #" + tournamentId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return reply(message.getChatId(), e.getMessage());
        }
    }

    private SendMessage reply(long chatId, String text) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
    }
}
