package chikagebb.linktracker.bot.handler;

import chikagebb.linktracker.bot.client.ScrapperClient;
import chikagebb.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommandHandler implements UpdateHandler {

    private final ScrapperClient scrapperClient;

    private static final String START_MESSAGE = """
        👋 Привет!

        Я помогу тебе следить за обновлениями по ссылкам 🔍

        Просто отправь:
        /track <ссылка>

        И я буду уведомлять тебя, если что-то изменится.

        📌 Команды:
        /track — добавить ссылку \s
        /untrack — удалить ссылку \s
        /list — список ссылок \s
        /help — помощь
        """;

    @Override
    public boolean isSupport(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }

        return TelegramCommand.START.isEnabled()
                && TelegramCommand.START.getValue().equals(update.message().text());
    }

    @Override
    public void handle(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();

        try {
            scrapperClient.registerChat(chatId);
        } catch (ResponseStatusException e) {

            log.atDebug()
                    .setMessage("chat.already.registered")
                    .addKeyValue("chat_id", chatId)
                    .addKeyValue("error_message", e.getMessage())
                    .log();
        }

        log.atInfo()
                .setMessage("User registered")
                .addKeyValue("chat_id", update.message().from().id())
                .log();

        bot.execute(new SendMessage(chatId, START_MESSAGE));
    }
}
