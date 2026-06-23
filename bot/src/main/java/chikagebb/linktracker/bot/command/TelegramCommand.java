package chikagebb.linktracker.bot.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TelegramCommand {
    START("/start", "Запустить бота", true),
    HELP("/help", "Помощь", true),
    TRACK("/track", "Начать отслеживание ссылки", true),
    UNTRACK("/untrack", "Прекратить отслеживание ссылки", true),
    LIST("/list", "Список отслеживаемых ссылок", true);

    private final String value;
    private final String description;
    private final boolean isEnabled;

    public static TelegramCommand fromValue(String value) {
        for (TelegramCommand command : values()) {
            if (command.value.equals(value)) {
                return command;
            }
        }
        return null;
    }
}
