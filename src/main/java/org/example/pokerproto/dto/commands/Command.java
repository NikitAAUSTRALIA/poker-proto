package org.example.pokerproto.dto.commands;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface Command {
    String command();
    BotApiMethod<?> handle(Message message);
    String description();
}
