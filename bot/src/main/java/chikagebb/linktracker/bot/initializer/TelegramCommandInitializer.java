package chikagebb.linktracker.bot.initializer;

import chikagebb.linktracker.bot.command.TelegramCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramCommandInitializer {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        BotCommand[] commands = Arrays.stream(TelegramCommand.values())
                .filter(TelegramCommand::isEnabled)
                .map(c -> new BotCommand(c.getValue(), c.getDescription()))
                .toArray(BotCommand[]::new);

        telegramBot.execute(new SetMyCommands(commands));

        log.atInfo().setMessage("Bot commands initialized successfully").log();
    }
}
