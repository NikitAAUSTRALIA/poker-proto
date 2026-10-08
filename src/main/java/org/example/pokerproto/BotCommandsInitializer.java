package org.example.pokerproto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.menubutton.SetChatMenuButton;
import org.telegram.telegrambots.meta.api.objects.menubutton.MenuButtonWebApp;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class BotCommandsInitializer {

    private final PokerBot bot;
    private final String webAppUrl;

    public BotCommandsInitializer(PokerBot bot,
                                  @Value("${telegram.bot.webapp-url:${telegram.bot.webhook-url:}}") String webAppUrl) {
        this.bot = bot;
        this.webAppUrl = webAppUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        try {
            bot.execute(new DeleteMyCommands());
            log.info("Bot commands menu cleared (DeleteMyCommands)");
        } catch (TelegramApiException e) {
            log.warn("Failed to clear bot commands (possibly offline or rate-limited): {}", e.getMessage());
        }

        if (webAppUrl != null && !webAppUrl.isBlank()) {
            try {
                bot.execute(SetChatMenuButton.builder()
                        .menuButton(MenuButtonWebApp.builder()
                                .text("Приложение")
                                .webAppInfo(new WebAppInfo(webAppUrl))
                                .build())
                        .build());
                log.info("Bot menu button set to WebApp");
            } catch (TelegramApiException e) {
                log.warn("Failed to set menu button: {}", e.getMessage());
            }
        }
    }
}
