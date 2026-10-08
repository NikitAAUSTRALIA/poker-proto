package org.example.pokerproto.webhook;

import lombok.extern.slf4j.Slf4j;
import org.example.pokerproto.PokerBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class WebhookInitializer {

    private final PokerBot bot;
    private final String fullUrl;
    private final String secret;

    public WebhookInitializer(PokerBot bot,
                              @Value("${telegram.bot.webhook-url}") String url,
                              @Value("${telegram.bot.webhook-path}") String path,
                              @Value("${telegram.bot.secret}") String secret) {
        this.bot = bot;
        this.fullUrl = url + path;
        this.secret = secret;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        log.info("Setting webhook to {}", fullUrl);

        SetWebhook set = SetWebhook.builder()
                .url(fullUrl)
                .secretToken(secret)
                .dropPendingUpdates(true)
                .build();

        try {
            bot.execute(set);
            log.info("Webhook set OK");
        } catch (TelegramApiException e) {
            log.warn("Failed to set webhook (possibly rate-limited 429 or offline): {}", e.getMessage());
        }
    }
}
