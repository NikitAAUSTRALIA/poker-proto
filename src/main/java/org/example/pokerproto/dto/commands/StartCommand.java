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
        String firstName = message.getFrom() != null && message.getFrom().getFirstName() != null
                ? message.getFrom().getFirstName()
                : "Гость";

        String text = "Добро пожаловать в SOVA POKER CLUB, " + firstName + "!\n\n"
                + "Здесь вы можете следить за расписанием живых турниров, записываться на игры в один клик и отслеживать свой рейтинг в сезоне.\n\n"
                + "Нажмите кнопку ниже, чтобы открыть клубное приложение:";

        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(text);

        if (webAppUrl != null && !webAppUrl.isBlank()) {
            builder.replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(
                            InlineKeyboardButton.builder()
                                    .text("♠️ Войти в SOVA CLUB")
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
