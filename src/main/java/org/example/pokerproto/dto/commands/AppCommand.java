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
public class AppCommand implements Command {

    private final String webAppUrl;

    public AppCommand(@Value("${telegram.bot.webapp-url:${telegram.bot.webhook-url:}}") String webAppUrl) {
        this.webAppUrl = webAppUrl;
    }

    @Override
    public String command() {
        return "/app";
    }

    @Override
    public String description() {
        return "Открыть покерное приложение";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(message.getChatId().toString());

        if (webAppUrl != null && !webAppUrl.isBlank()) {
            builder.text("Нажмите кнопку ниже, чтобы открыть покерное приложение:")
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboardRow(List.of(
                                    InlineKeyboardButton.builder()
                                            .text("Открыть приложение")
                                            .webApp(new WebAppInfo(webAppUrl))
                                            .build()
                            ))
                            .build());
        } else {
            builder.text("URL веб-приложения не настроен.");
        }

        return builder.build();
    }
}
