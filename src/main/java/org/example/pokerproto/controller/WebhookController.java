package org.example.pokerproto.controller;

import org.example.pokerproto.PokerBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    private final PokerBot bot;
    private final String secret;

    public WebhookController(PokerBot bot,
                             @Value("${telegram.bot.secret}") String secret) {
        this.bot = bot;
        this.secret = secret;
    }

    @PostMapping("${telegram.bot.webhook-path}")
    public ResponseEntity<BotApiMethod<?>> onUpdate(
            @RequestBody Update update,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String token) {
        if (!secret.equals(token)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(bot.onWebhookUpdateReceived(update));
    }
}
