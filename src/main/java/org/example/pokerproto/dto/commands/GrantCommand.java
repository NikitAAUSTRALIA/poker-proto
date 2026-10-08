package org.example.pokerproto.dto.commands;

import org.example.pokerproto.database.entity.User;
import org.example.pokerproto.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

@Component
public class GrantCommand implements Command {

    private final UserService userService;

    public GrantCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String command() {
        return "/grant";
    }

    @Override
    public BotApiMethod<?> handle(Message message) {
        Long senderId = message.getFrom() != null ? message.getFrom().getId() : null;
        if (!userService.isAdmin(senderId)) {
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("У вас нет прав для выполнения этой команды")
                    .build();
        }

        String[] parts = message.getText().trim().split("\\s+");
        if (parts.length < 2) {
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("Формат команды: /grant <tg_id | @username>")
                    .build();
        }

        String target = parts[1].trim();

        if (target.startsWith("@")) {
            String username = target.substring(1).trim();
            if (username.isEmpty()) {
                return SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text("Укажите username после символа @, например: /grant @username")
                        .build();
            }
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text("Пользователь @" + username + " не найден в базе данных. Пользователь должен предварительно отправить команду /register в боте")
                        .build();
            }
            User user = userOpt.get();
            userService.grantAdmin(user.getTgId());
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("Пользователю @" + username + " (ID: " + user.getTgId() + ") успешно выданы права администратора")
                    .build();
        }

        try {
            Long targetId = Long.parseLong(target);
            userService.grantAdmin(targetId);
            Optional<User> userOpt = userService.findByTgId(targetId);
            String displayName = userOpt.filter(u -> u.getUsername() != null && !u.getUsername().isBlank())
                    .map(u -> "@" + u.getUsername() + " (ID: " + targetId + ")")
                    .orElse(String.valueOf(targetId));
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("Пользователю " + displayName + " успешно выданы права администратора")
                    .build();
        } catch (NumberFormatException e) {
            Optional<User> userOpt = userService.findByUsername(target);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userService.grantAdmin(user.getTgId());
                String userLabel = user.getUsername() != null ? "@" + user.getUsername() : String.valueOf(user.getTgId());
                return SendMessage.builder()
                        .chatId(message.getChatId().toString())
                        .text("Пользователю " + userLabel + " (ID: " + user.getTgId() + ") успешно выданы права администратора")
                        .build();
            }
            return SendMessage.builder()
                    .chatId(message.getChatId().toString())
                    .text("Некорректный идентификатор. Укажите числовой tg_id или @username пользователя")
                    .build();
        }
    }

    @Override
    public String description() {
        return "Выдать права администратора по tg_id или @username";
    }
}
