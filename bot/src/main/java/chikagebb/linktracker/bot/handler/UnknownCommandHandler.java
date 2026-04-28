package chikagebb.linktracker.bot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UnknownCommandHandler implements UpdateHandler {

    private static final String UNKNOWN_COMMAND_MESSAGE = """
        ❓ Неизвестная команда

        Нажми /help, чтобы посмотреть список доступных команд
        """;

    @Override
    public boolean isSupport(Update update) {
        return update.message() != null || update.message().text() != null;
    }

    @Override
    public void handle(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();
        log.atWarn()
                .setMessage("Пользователь вызвал неизвестную команду")
                .addKeyValue("user_id", update.message().from().id())
                .addKeyValue("text", update.message().text())
                .log();

        bot.execute(new SendMessage(chatId, UNKNOWN_COMMAND_MESSAGE));
    }
}
