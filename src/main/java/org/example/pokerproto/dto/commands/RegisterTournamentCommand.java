package org.example.pokerproto.dto.commands;

import lombok.RequiredArgsConstructor;
import org.example.pokerproto.database.entity.Tournament;
import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.service.TournamentService;
import org.example.pokerproto.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class RegisterTournamentCommand implements Command {

    private final UserService userService;
    private final TournamentService tournamentService;

    @Override
    public String command() {
        return "/register_tournament";
    }

    @Override
    public String description() {
        return "Создать турнир (админ): /register_tournament <название> | <excel_url>";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        var from = message.getFrom();

        if (!userService.isAdmin(from.getId())) {
            return reply(message.getChatId(), "Только для админов.");
        }

        String text = message.getText().trim();
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            return reply(message.getChatId(),
                    "Использование: /register_tournament <название> | <excel_url>");
        }

        String[] args = parts[1].split("\\|", 2);
        if (args.length < 2) {
            return reply(message.getChatId(),
                    "Раздели название и ссылку символом |");
        }

        String title = args[0].trim();
        String url = args[1].trim();

        if (title.isEmpty() || url.isEmpty()) {
            return reply(message.getChatId(), "Название и ссылка не должны быть пустыми.");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return reply(message.getChatId(), "Ссылка должна начинаться с http:// или https://");
        }

        User admin = userService.findByTgId(from.getId()).orElseThrow();
        Tournament t = tournamentService.create(title, url, admin);

        return reply(message.getChatId(),
                "Турнир создан. ID: " + t.getId() + "\nНазвание: " + t.getTitle()
                        + "\nЗаписаться: /tournament_reg " + t.getId());
    }

    private SendMessage reply(long chatId, String text) {
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();
    }
}
