package chikagebb.linktracker.bot.handler;

import chikagebb.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelpCommandHandler implements UpdateHandler {

    @Override
    public boolean isSupport(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return false;
        }

        return TelegramCommand.HELP.isEnabled()
                && TelegramCommand.HELP.getValue().equals(update.message().text());
    }

    @Override
    public void handle(Update update, TelegramBot bot) {
        Long chatId = update.message().chat().id();

        log.atInfo()
                .setMessage("User calls /help command")
                .addKeyValue("user_id", update.message().from().id())
                .log();

        String commandList = Arrays.stream(TelegramCommand.values())
                .filter(TelegramCommand::isEnabled)
                .map(c -> c.getValue() + " - " + c.getDescription())
                .collect(Collectors.joining("\n"));

        bot.execute(new SendMessage(chatId, "Доступные команды: \n" + commandList));
    }
}
