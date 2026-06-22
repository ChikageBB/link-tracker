package chikagebb.linktracker.bot.controller;

import chikagebb.linktracker.bot.model.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UpdateController {

    private final TelegramBot telegramBot;

    @PostMapping("/updates")
    public ResponseEntity<Void> sendUpdate(@Validated @RequestBody LinkUpdate update) {

        if (update.getId() == null
                || update.getUrl() == null
                || update.getTgChatIds() == null
                || update.getTgChatIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Отсутствует валидация");
        }

        log.atInfo()
                .setMessage("update received")
                .addKeyValue("url", update.getUrl())
                .addKeyValue("chatIds", update.getTgChatIds())
                .log();

        for (Long chatId : update.getTgChatIds()) {
            telegramBot.execute(new SendMessage(chatId, update.getDescription()));
        }

        return ResponseEntity.ok().build();
    }
}
