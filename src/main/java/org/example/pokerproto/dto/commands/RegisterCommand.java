package org.example.pokerproto.dto.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pokerproto.database.entity.Role;
import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterCommand implements Command {

    private final UserService userService;

    @Override
    public String command() {
        return "/register";
    }

    @Override
    public String description() {
        return "Зарегистрироваться в боте";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        var from = message.getFrom();

        User user = userService.registerOrUpdate(
                from.getId(),
                from.getUserName(),
                from.getFirstName(),
                from.getLastName()
        );

        String roleText = user.getRole() == Role.ADMIN
                ? " (админ)" : "";

        return SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("Ты зарегистрирован, " + from.getFirstName() + roleText + "!")
                .build();
    }
}
