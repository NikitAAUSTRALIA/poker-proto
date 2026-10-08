package org.example.pokerproto.dto.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;

import java.util.List;

@Component
public class StartCommand implements Command {

    private final String webAppUrl;

    public StartCommand(@Value("${telegram.bot.webapp-url:${telegram.bot.webhook-url:}}") String webAppUrl) {
        this.webAppUrl = webAppUrl;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("Здравствуйте! Добро пожаловать в покерный клуб.\n\n"
                        + "Доступные команды:\n"
                        + "/app — открыть покерное приложение\n"
                        + "/register — зарегистрироваться в боте\n"
                        + "/tournaments — список турниров\n"
                        + "/tournament_reg <id> — записаться на турнир\n"
                        + "/tournament_unreg <id> — отписаться от турнира\n"
                        + "/leaderboard — рейтинг игроков сезона");

        if (webAppUrl != null && !webAppUrl.isBlank()) {
            builder.replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder()
                                    .text("Открыть приложение")
                                    .webApp(new WebAppInfo(webAppUrl))
                                    .build()
                    ))
                    .build());
        }

        return builder.build();
    }

    @Override
    public String description() {
        return "Начало работы бота";
    }
}
